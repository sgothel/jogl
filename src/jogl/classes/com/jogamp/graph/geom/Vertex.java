/**
 * Copyright 2011-2023 JogAmp Community. All rights reserved.
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

import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vert3fImmutable;

/**
 * A Vertex exposing Vec3f vertex- and texture-coordinates.
 */
public final class Vertex implements Vert3fImmutable {
    private int id;
    private boolean onCurve;
    private final Vec3f coord = new Vec3f();
    private final Vec3f texCoord = new Vec3f();

    public Vertex() {
        this.id = Integer.MAX_VALUE;
    }

    /** Copy ctor */
    public Vertex(final Vertex src) {
        this.id = Integer.MAX_VALUE;
        coord.set(src.getCoord());
        texCoord.set(src.getTexCoord());
        setOnCurve(src.isOnCurve());
    }

    public Vertex(final int id, final boolean onCurve, final Vec3f texCoord) {
        this.id = id;
        this.onCurve = onCurve;
        this.texCoord.set(texCoord);
    }

    public Vertex(final int id, final boolean onCurve, final float texCoordX, final float texCoordY, final float texCoordZ) {
        this.id = id;
        this.onCurve = onCurve;
        this.texCoord.set(texCoordX, texCoordY, texCoordZ);
    }

    public Vertex(final Vec3f coord, final boolean onCurve) {
        this.id = Integer.MAX_VALUE;
        this.coord.set(coord);
        setOnCurve(onCurve);
    }

    public Vertex(final Vec2f coord, final boolean onCurve) {
        this.id = Integer.MAX_VALUE;
        this.coord.set(coord, 0f);
        setOnCurve(onCurve);
    }

    public Vertex(final float x, final float y, final boolean onCurve) {
        this(x, y, 0, onCurve);
    }

    public Vertex(final float[] coordsBuffer, final int offset, final int length, final boolean onCurve) {
        this(coordsBuffer[offset+0], coordsBuffer[offset+1], 2 < length ? coordsBuffer[offset+2] : 0f, onCurve);
    }

    public Vertex(final float x, final float y, final float z, final boolean onCurve) {
        this.id = Integer.MAX_VALUE;
        coord.set(x, y, z);
        setOnCurve(onCurve);
    }

    public final void setCoord(final Vec3f coord) {
        this.coord.set(coord);
    }

    public void setCoord(final Vec2f coord) {
        this.coord.set(coord, 0f);
    }

    public final void setCoord(final float x, final float y, final float z) {
        coord.set(x, y, z);
    }

    public final void setCoord(final float x, final float y) {
        coord.set(x, y, 0f);
    }

    @Override
    public int getCoordCount() {
        return 3;
    }

    @Override
    public final Vec3f getCoord() {
        return coord;
    }

    public final void setX(final float x) {
        coord.setX(x);
    }

    public final void setY(final float y) {
        coord.setY(y);
    }

    public final void setZ(final float z) {
        coord.setZ(z);
    }

    @Override
    public final float x() {
        return coord.x();
    }

    @Override
    public final float y() {
        return coord.y();
    }

    @Override
    public final float z() {
        return coord.z();
    }

    public final boolean isOnCurve() {
        return onCurve;
    }

    public final void setOnCurve(final boolean onCurve) {
        this.onCurve = onCurve;
    }

    public final int getId(){
        return id;
    }

    public final void setId(final int id){
        this.id = id;
    }

    /**
     * @param obj the Object to compare this Vertex with
     * @return true if {@code obj} is a Vertex and not null, on-curve flag is equal and has same vertex- and tex-coords.
     */
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
               getTexCoord().isEqual( v.getTexCoord() ) &&
               getCoord().isEqual( v.getCoord() );
    }

    @Override
    public final int hashCode() {
        throw new InternalError("hashCode not designed");
    }

    public final Vec3f getTexCoord() {
        return texCoord;
    }

    public final void setTexCoord(final Vec3f v) {
        texCoord.set(v);
    }

    public final void setTexCoord(final float s, final float t, final float p) {
        texCoord.set(s, t, p);
    }

    /**
     * @return deep copy of this Vertex element via {@link Vertex#Vertex(Vertex)}
     */
    public Vertex copy(){
        return new Vertex(this);
    }

    @Override
    public String toString() {
        return "[ID: " + id + ", onCurve: " + onCurve +
               ": p " + coord +
               ", t " + texCoord + "]";
    }
}
