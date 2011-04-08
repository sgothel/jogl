/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

/**
 * A Vertex with custom memory layout using custom factory. 
 */
public interface Vertex extends Comparable<Vertex>, Cloneable {

    public static interface Factory <T extends Vertex> {
        T create();

        T create(float x, float y);

        T create(float x, float y, float z);

        T create(float[] coordsBuffer, int offset, int length);    
    }
    
    void setCoord(float x, float y);

    void setCoord(float x, float y, float z);

    void setCoord(float[] coordsBuffer, int offset, int length);
    
    float[] getCoord();

    void setX(float x);

    void setY(float y);

    void setZ(float z);

    float getX();

    float getY();

    float getZ();

    boolean isOnCurve();

    void setOnCurve(boolean onCurve);

    int getId();
    
    void setId(int id);
    
    int compareTo(Vertex p);
    
    float[] getTexCoord();
    
    void setTexCoord(float s, float t);
    
    Vertex clone();
}
