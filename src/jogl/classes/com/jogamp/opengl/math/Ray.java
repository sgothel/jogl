/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.math;

import com.jogamp.opengl.math.geom.AABBox;

/**
 * Simple compound denoting a ray.
 * <p>
 * A ray, also known as a half line, consists out of it's <i>origin</i>
 * and <i>direction</i>. Hence it is bound to only the <i>origin</i> side,
 * where the other end is +infinitive.
 * <pre>
 * R(t) = R0 + Rd * t with R0 origin, Rd direction and t > 0.0
 * </pre>
 * </p>
 * <p>
 * A {@link Ray} maybe used for <i>picking</i>
 * using a {@link AABBox bounding box} via
 * {@link AABBox#intersectsRay(Ray) fast probe} or
 * {@link AABBox#getRayIntersection(float[], Ray, float, boolean, float[], float[], float[]) returning the intersection}.
 * </p>
 */
public class Ray {
    /** Origin of Ray, float[3]. */
    public final float[] orig = new float[3];

    /** Normalized direction vector of ray, float[3]. */
    public final float[] dir = new float[3];

    public String toString() {
        return "Ray[orig["+orig[0]+", "+orig[1]+", "+orig[2]+"], dir["+dir[0]+", "+dir[1]+", "+dir[2]+"]]";
    }
}