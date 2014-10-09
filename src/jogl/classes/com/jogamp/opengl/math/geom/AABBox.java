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
package com.jogamp.opengl.math.geom;

import jogamp.graph.geom.plane.AffineTransform;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.Ray;
import com.jogamp.opengl.math.VectorUtil;


/**
 * Axis Aligned Bounding Box. Defined by two 3D coordinates (low and high)
 * The low being the the lower left corner of the box, and the high being the upper
 * right corner of the box.
 * <p>
 * A few references for collision detection, intersections:
 * <pre>
 * http://www.realtimerendering.com/intersections.html
 * http://www.codercorner.com/RayAABB.cpp
 * http://www.siggraph.org/education/materials/HyperGraph/raytrace/rtinter0.htm
 * http://realtimecollisiondetection.net/files/levine_swept_sat.txt
 * </pre>
 * </p>
 *
 */
public class AABBox {
    private static final boolean DEBUG = FloatUtil.DEBUG;
    private final float[] low = new float[3];
    private final float[] high = new float[3];
    private final float[] center = new float[3];

    /**
     * Create an Axis Aligned bounding box (AABBox)
     * where the low and and high MAX float Values.
     */
    public AABBox() {
        reset();
    }

    /**
     * Create an AABBox copying all values from the given one
     * @param src the box value to be used for the new instance
     */
    public AABBox(final AABBox src) {
        copy(src);
    }

    /**
     * Create an AABBox specifying the coordinates
     * of the low and high
     * @param lx min x-coordinate
     * @param ly min y-coordnate
     * @param lz min z-coordinate
     * @param hx max x-coordinate
     * @param hy max y-coordinate
     * @param hz max z-coordinate
     */
    public AABBox(final float lx, final float ly, final float lz,
                  final float hx, final float hy, final float hz) {
        setSize(lx, ly, lz, hx, hy, hz);
    }

    /**
     * Create a AABBox defining the low and high
     * @param low min xyz-coordinates
     * @param high max xyz-coordinates
     */
    public AABBox(final float[] low, final float[] high) {
        setSize(low, high);
    }

    /**
     * resets this box to the inverse low/high, allowing the next {@link #resize(float, float, float)} command to hit.
     * @return this AABBox for chaining
     */
    public final AABBox reset() {
        setLow(Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE);
        setHigh(-1*Float.MAX_VALUE,-1*Float.MAX_VALUE,-1*Float.MAX_VALUE);
        center[0] = 0f;
        center[1] = 0f;
        center[2] = 0f;
        return this;
    }

    /** Get the max xyz-coordinates
     * @return a float array containing the max xyz coordinates
     */
    public final float[] getHigh() {
        return high;
    }

    private final void setHigh(final float hx, final float hy, final float hz) {
        this.high[0] = hx;
        this.high[1] = hy;
        this.high[2] = hz;
    }

    /** Get the min xyz-coordinates
     * @return a float array containing the min xyz coordinates
     */
    public final float[] getLow() {
        return low;
    }

    private final void setLow(final float lx, final float ly, final float lz) {
        this.low[0] = lx;
        this.low[1] = ly;
        this.low[2] = lz;
    }

    private final void computeCenter() {
        center[0] = (high[0] + low[0])/2f;
        center[1] = (high[1] + low[1])/2f;
        center[2] = (high[2] + low[2])/2f;
    }

    /**
     * Copy given AABBox 'src' values to this AABBox.
     *
     * @param src source AABBox
     * @return this AABBox for chaining
     */
    public final AABBox copy(final AABBox src) {
        System.arraycopy(src.low, 0, low, 0, 3);
        System.arraycopy(src.high, 0, high, 0, 3);
        System.arraycopy(src.center, 0, center, 0, 3);
        return this;
    }

    /**
     * Set size of the AABBox specifying the coordinates
     * of the low and high.
     *
     * @param low min xyz-coordinates
     * @param high max xyz-coordinates
     * @return this AABBox for chaining
     */
    public final AABBox setSize(final float[] low, final float[] high) {
        return setSize(low[0],low[1],low[2], high[0],high[1],high[2]);
    }

    /**
     * Set size of the AABBox specifying the coordinates
     * of the low and high.
     *
     * @param lx min x-coordinate
     * @param ly min y-coordnate
     * @param lz min z-coordinate
     * @param hx max x-coordinate
     * @param hy max y-coordinate
     * @param hz max z-coordinate
     * @return this AABBox for chaining
     */
    public final AABBox setSize(final float lx, final float ly, final float lz,
                                final float hx, final float hy, final float hz) {
        this.low[0] = lx;
        this.low[1] = ly;
        this.low[2] = lz;
        this.high[0] = hx;
        this.high[1] = hy;
        this.high[2] = hz;
        computeCenter();
        return this;
    }

    /**
     * Resize the AABBox to encapsulate another AABox
     * @param newBox AABBox to be encapsulated in
     * @return this AABBox for chaining
     */
    public final AABBox resize(final AABBox newBox) {
        final float[] newLow = newBox.getLow();
        final float[] newHigh = newBox.getHigh();

        /** test low */
        if (newLow[0] < low[0])
            low[0] = newLow[0];
        if (newLow[1] < low[1])
            low[1] = newLow[1];
        if (newLow[2] < low[2])
            low[2] = newLow[2];

        /** test high */
        if (newHigh[0] > high[0])
            high[0] = newHigh[0];
        if (newHigh[1] > high[1])
            high[1] = newHigh[1];
        if (newHigh[2] > high[2])
            high[2] = newHigh[2];

        computeCenter();
        return this;
    }

    /**
     * Resize the AABBox to encapsulate another AABox, which will be <i>transformed</i> on the fly first.
     * @param newBox AABBox to be encapsulated in
     * @param t the {@link AffineTransform} applied on <i>newBox</i> on the fly
     * @param tmpV3 temp float[3] storage
     * @return this AABBox for chaining
     */
    public final AABBox resize(final AABBox newBox, final AffineTransform t, final float[] tmpV3) {
        /** test low */
        {
            final float[] newBoxLow = newBox.getLow();
            t.transform(newBoxLow, tmpV3);
            tmpV3[2] = newBoxLow[2];
            if (tmpV3[0] < low[0])
                low[0] = tmpV3[0];
            if (tmpV3[1] < low[1])
                low[1] = tmpV3[1];
            if (tmpV3[2] < low[2])
                low[2] = tmpV3[2];
        }

        /** test high */
        {
            final float[] newBoxHigh = newBox.getHigh();
            t.transform(newBoxHigh, tmpV3);
            tmpV3[2] = newBoxHigh[2];
            if (tmpV3[0] > high[0])
                high[0] = tmpV3[0];
            if (tmpV3[1] > high[1])
                high[1] = tmpV3[1];
            if (tmpV3[2] > high[2])
                high[2] = tmpV3[2];
        }

        computeCenter();
        return this;
    }

    /**
     * Resize the AABBox to encapsulate the passed
     * xyz-coordinates.
     * @param x x-axis coordinate value
     * @param y y-axis coordinate value
     * @param z z-axis coordinate value
     * @return this AABBox for chaining
     */
    public final AABBox resize(final float x, final float y, final float z) {
        /** test low */
        if (x < low[0]) {
            low[0] = x;
        }
        if (y < low[1]) {
            low[1] = y;
        }
        if (z < low[2]) {
            low[2] = z;
        }

        /** test high */
        if (x > high[0]) {
            high[0] = x;
        }
        if (y > high[1]) {
            high[1] = y;
        }
        if (z > high[2]) {
            high[2] = z;
        }

        computeCenter();
        return this;
    }

    /**
     * Resize the AABBox to encapsulate the passed
     * xyz-coordinates.
     * @param xyz xyz-axis coordinate values
     * @param offset of the array
     * @return this AABBox for chaining
     */
    public final AABBox resize(final float[] xyz, final int offset) {
        return resize(xyz[0+offset], xyz[1+offset], xyz[2+offset]);
    }

    /**
     * Resize the AABBox to encapsulate the passed
     * xyz-coordinates.
     * @param xyz xyz-axis coordinate values
     * @return this AABBox for chaining
     */
    public final AABBox resize(final float[] xyz) {
        return resize(xyz[0], xyz[1], xyz[2]);
    }

    /**
     * Check if the x & y coordinates are bounded/contained
     * by this AABBox
     * @param x  x-axis coordinate value
     * @param y  y-axis coordinate value
     * @return true if  x belong to (low.x, high.x) and
     * y belong to (low.y, high.y)
     */
    public final boolean contains(final float x, final float y) {
        if(x<low[0] || x>high[0]){
            return false;
        }
        if(y<low[1]|| y>high[1]){
            return false;
        }
        return true;
    }

    /**
     * Check if the xyz coordinates are bounded/contained
     * by this AABBox.
     * @param x x-axis coordinate value
     * @param y y-axis coordinate value
     * @param z z-axis coordinate value
     * @return true if  x belong to (low.x, high.x) and
     * y belong to (low.y, high.y) and  z belong to (low.z, high.z)
     */
    public final boolean contains(final float x, final float y, final float z) {
        if(x<low[0] || x>high[0]){
            return false;
        }
        if(y<low[1]|| y>high[1]){
            return false;
        }
        if(z<low[2] || z>high[2]){
            return false;
        }
        return true;
    }

    /**
     * Check if there is a common region between this AABBox and the passed
     * 2D region irrespective of z range
     * @param x lower left x-coord
     * @param y lower left y-coord
     * @param w width
     * @param h hight
     * @return true if this AABBox might have a common region with this 2D region
     */
    public final boolean intersects2DRegion(final float x, final float y, final float w, final float h) {
        if (w <= 0 || h <= 0) {
            return false;
        }

        final float _w = getWidth();
        final float _h = getHeight();
        if (_w <= 0 || _h <= 0) {
            return false;
        }

        final float x0 = getMinX();
        final float y0 = getMinY();
        return (x + w > x0 &&
                y + h > y0 &&
                x < x0 + _w &&
                y < y0 + _h);
    }

    /**
     * Check if {@link Ray} intersects this bounding box.
     * <p>
     * Versions uses the SAT[1], testing 6 axes.
     * Original code for OBBs from MAGIC.
     * Rewritten for AABBs and reorganized for early exits[2].
     * </p>
     * <pre>
     * [1] SAT = Separating Axis Theorem
     * [2] http://www.codercorner.com/RayAABB.cpp
     * </pre>
     * @param ray
     * @return
     */
    public final boolean intersectsRay(final Ray ray) {
        // diff[XYZ] -> VectorUtil.subVec3(diff, ray.orig, center);
        //  ext[XYZ] -> extend VectorUtil.subVec3(ext, high, center);

        final float dirX  = ray.dir[0];
        final float diffX = ray.orig[0] - center[0];
        final float extX  = high[0] - center[0];
        if( Math.abs(diffX) > extX && diffX*dirX >= 0f ) return false;

        final float dirY  = ray.dir[1];
        final float diffY = ray.orig[1] - center[1];
        final float extY  = high[1] - center[1];
        if( Math.abs(diffY) > extY && diffY*dirY >= 0f ) return false;

        final float dirZ  = ray.dir[2];
        final float diffZ = ray.orig[2] - center[2];
        final float extZ  = high[2] - center[2];
        if( Math.abs(diffZ) > extZ && diffZ*dirZ >= 0f ) return false;

        final float absDirY = Math.abs(dirY);
        final float absDirZ = Math.abs(dirZ);

        float f = dirY * diffZ - dirZ * diffY;
        if( Math.abs(f) > extY*absDirZ + extZ*absDirY ) return false;

        final float absDirX = Math.abs(dirX);

        f = dirZ * diffX - dirX * diffZ;
        if( Math.abs(f) > extX*absDirZ + extZ*absDirX ) return false;

        f = dirX * diffY - dirY * diffX;
        if( Math.abs(f) > extX*absDirY + extY*absDirX ) return false;

        return true;
    }

    /**
     * Return intersection of a {@link Ray} with this bounding box,
     * or null if none exist.
     * <p>
     * <ul>
     *  <li>Original code by Andrew Woo, from "Graphics Gems", Academic Press, 1990 [2]</li>
     *  <li>Optimized code by Pierre Terdiman, 2000 (~20-30% faster on my Celeron 500)</li>
     *  <li>Epsilon value added by Klaus Hartmann.</li>
     * </ul>
     * </p>
     * <p>
     * Method is based on the requirements:
     * <ul>
     *  <li>the integer representation of 0.0f is 0x00000000</li>
     *  <li>the sign bit of the float is the most significant one</li>
     * </ul>
     * </p>
     * <p>
     * Report bugs: p.terdiman@codercorner.com (original author)
     * </p>
     * <pre>
     * [1] http://www.codercorner.com/RayAABB.cpp
     * [2] http://tog.acm.org/resources/GraphicsGems/gems/RayBox.c
     * </pre>
     * @param result vec3
     * @param ray
     * @param epsilon
     * @param assumeIntersection if true, method assumes an intersection, i.e. by pre-checking via {@link #intersectsRay(Ray)}.
     *                           In this case method will not validate a possible non-intersection and just computes
     *                           coordinates.
     * @param tmp1V3 temp vec3
     * @param tmp2V3 temp vec3
     * @param tmp3V3 temp vec3
     * @return float[3] result of intersection coordinates, or null if none exists
     */
    public final float[] getRayIntersection(final float[] result, final Ray ray, final float epsilon,
                                            final boolean assumeIntersection,
                                            final float[] tmp1V3, final float[] tmp2V3, final float[] tmp3V3) {
        final float[] maxT = { -1f, -1f, -1f };

        final float[] origin = ray.orig;
        final float[] dir = ray.dir;

        boolean inside = true;

        // Find candidate planes.
        for(int i=0; i<3; i++) {
            if(origin[i] < low[i]) {
                result[i] = low[i];
                inside    = false;

                // Calculate T distances to candidate planes
                if( 0 != Float.floatToIntBits(dir[i]) ) {
                    maxT[i] = (low[i] - origin[i]) / dir[i];
                }
            } else if(origin[i] > high[i]) {
                result[i] = high[i];
                inside    = false;

                // Calculate T distances to candidate planes
                if( 0 != Float.floatToIntBits(dir[i]) ) {
                    maxT[i] = (high[i] - origin[i]) / dir[i];
                }
            }
        }

        // Ray origin inside bounding box
        if(inside) {
            System.arraycopy(origin, 0, result, 0, 3);
            return result;
        }

        // Get largest of the maxT's for final choice of intersection
        int whichPlane = 0;
        if(maxT[1] > maxT[whichPlane]) { whichPlane = 1; }
        if(maxT[2] > maxT[whichPlane]) { whichPlane = 2; }

        if( !assumeIntersection ) {
            // Check final candidate actually inside box
            if( 0 != ( Float.floatToIntBits(maxT[whichPlane]) & 0x80000000 ) ) {
                return null;
            }

            /** Use unrolled version below ..
            for(int i=0; i<3; i++) {
                if( i!=whichPlane ) {
                    result[i] = origin[i] + maxT[whichPlane] * dir[i];
                    if(result[i] < minB[i] - epsilon || result[i] > maxB[i] + epsilon) { return null; }
                    // if(result[i] < minB[i] || result[i] > maxB[i] ) { return null; }
                }
            } */
            switch( whichPlane ) {
                case 0:
                    result[1] = origin[1] + maxT[whichPlane] * dir[1];
                    if(result[1] < low[1] - epsilon || result[1] > high[1] + epsilon) { return null; }
                    result[2] = origin[2] + maxT[whichPlane] * dir[2];
                    if(result[2] < low[2] - epsilon || result[2] > high[2] + epsilon) { return null; }
                    break;
                case 1:
                    result[0] = origin[0] + maxT[whichPlane] * dir[0];
                    if(result[0] < low[0] - epsilon || result[0] > high[0] + epsilon) { return null; }
                    result[2] = origin[2] + maxT[whichPlane] * dir[2];
                    if(result[2] < low[2] - epsilon || result[2] > high[2] + epsilon) { return null; }
                    break;
                case 2:
                    result[0] = origin[0] + maxT[whichPlane] * dir[0];
                    if(result[0] < low[0] - epsilon || result[0] > high[0] + epsilon) { return null; }
                    result[1] = origin[1] + maxT[whichPlane] * dir[1];
                    if(result[1] < low[1] - epsilon || result[1] > high[1] + epsilon) { return null; }
                    break;
                default:
                    throw new InternalError("XXX");
            }
        } else {
            switch( whichPlane ) {
                case 0:
                    result[1] = origin[1] + maxT[whichPlane] * dir[1];
                    result[2] = origin[2] + maxT[whichPlane] * dir[2];
                    break;
                case 1:
                    result[0] = origin[0] + maxT[whichPlane] * dir[0];
                    result[2] = origin[2] + maxT[whichPlane] * dir[2];
                    break;
                case 2:
                    result[0] = origin[0] + maxT[whichPlane] * dir[0];
                    result[1] = origin[1] + maxT[whichPlane] * dir[1];
                    break;
                default:
                    throw new InternalError("XXX");
            }
        }
        return result; // ray hits box
    }

    /**
     * Get the size of this AABBox where the size is represented by the
     * length of the vector between low and high.
     * @return a float representing the size of the AABBox
     */
    public final float getSize() {
        return VectorUtil.distVec3(low, high);
    }

    /**
     * Get the Center of this AABBox
     * @return the xyz-coordinates of the center of the AABBox
     */
    public final float[] getCenter() {
        return center;
    }

    /**
     * Scale this AABBox by a constant
     * @param size a constant float value
     * @param tmpV3 caller provided temporary 3-component vector
     * @return this AABBox for chaining
     */
    public final AABBox scale(final float size, final float[] tmpV3) {
        tmpV3[0] = high[0] - center[0];
        tmpV3[1] = high[1] - center[1];
        tmpV3[2] = high[2] - center[2];

        VectorUtil.scaleVec3(tmpV3, tmpV3, size); // in-place scale
        VectorUtil.addVec3(high, center, tmpV3);

        tmpV3[0] = low[0] - center[0];
        tmpV3[1] = low[1] - center[1];
        tmpV3[2] = low[2] - center[2];

        VectorUtil.scaleVec3(tmpV3, tmpV3, size); // in-place scale
        VectorUtil.addVec3(low, center, tmpV3);
        return this;
    }

    /**
     * Translate this AABBox by a float[3] vector
     * @param t the float[3] translation vector
     * @return this AABBox for chaining
     */
    public final AABBox translate(final float[] t) {
        VectorUtil.addVec3(low, low, t); // in-place translate
        VectorUtil.addVec3(high, high, t); // in-place translate
        computeCenter();
        return this;
    }

    /**
     * Rotate this AABBox by a float[3] vector
     * @param quat the {@link Quaternion} used for rotation
     * @return this AABBox for chaining
     */
    public final AABBox rotate(final Quaternion quat) {
        quat.rotateVector(low, 0, low, 0);
        quat.rotateVector(high, 0, high, 0);
        computeCenter();
        return this;
    }

    public final float getMinX() {
        return low[0];
    }

    public final float getMinY() {
        return low[1];
    }

    public final float getMinZ() {
        return low[2];
    }

    public final float getMaxX() {
        return high[0];
    }

    public final float getMaxY() {
        return high[1];
    }

    public final float getMaxZ() {
        return high[2];
    }

    public final float getWidth(){
        return high[0] - low[0];
    }

    public final float getHeight() {
        return high[1] - low[1];
    }

    public final float getDepth() {
        return high[2] - low[2];
    }

    @Override
    public final boolean equals(final Object obj) {
        if( obj == this ) {
            return true;
        }
        if( null == obj || !(obj instanceof AABBox) ) {
            return false;
        }
        final AABBox other = (AABBox) obj;
        return VectorUtil.isVec2Equal(low, 0, other.low, 0, FloatUtil.EPSILON) &&
               VectorUtil.isVec3Equal(high, 0, other.high, 0, FloatUtil.EPSILON) ;
    }
    @Override
    public final int hashCode() {
        throw new InternalError("hashCode not designed");
    }

    /**
     * Assume this bounding box as being in object space and
     * compute the window bounding box.
     * <p>
     * If <code>useCenterZ</code> is <code>true</code>,
     * only 4 {@link FloatUtil#mapObjToWinCoords(float, float, float, float[], int[], int, float[], int, float[], float[]) mapObjToWinCoords}
     * operations are made on points [1..4] using {@link #getCenter()}'s z-value.
     * Otherwise 8 {@link FloatUtil#mapObjToWinCoords(float, float, float, float[], int[], int, float[], int, float[], float[]) mapObjToWinCoords}
     * operation on all 8 points are performed.
     * </p>
     * <pre>
     *  [2] ------ [4]
     *   |          |
     *   |          |
     *  [1] ------ [3]
     * </pre>
     * @param mat4PMv P x Mv matrix
     * @param view
     * @param useCenterZ
     * @param vec3Tmp0 3 component vector for temp storage
     * @param vec4Tmp1 4 component vector for temp storage
     * @param vec4Tmp2 4 component vector for temp storage
     * @return
     */
    public AABBox mapToWindow(final AABBox result, final float[/*16*/] mat4PMv, final int[] view, final boolean useCenterZ,
                              final float[] vec3Tmp0, final float[] vec4Tmp1, final float[] vec4Tmp2) {
        {
            // System.err.printf("AABBox.mapToWindow.0: view[%d, %d, %d, %d], this %s%n", view[0], view[1], view[2], view[3], toString());
            final float objZ = useCenterZ ? center[2] : getMinZ();
            FloatUtil.mapObjToWinCoords(getMinX(), getMinY(), objZ, mat4PMv, view, 0, vec3Tmp0, 0, vec4Tmp1, vec4Tmp2);
            // System.err.printf("AABBox.mapToWindow.p1: %f, %f, %f -> %f, %f, %f%n", getMinX(), getMinY(), objZ, vec3Tmp0[0], vec3Tmp0[1], vec3Tmp0[2]);
            // System.err.println("AABBox.mapToWindow.p1:");
            // System.err.println(FloatUtil.matrixToString(null, "  mat4PMv", "%10.5f", mat4PMv, 0, 4, 4, false /* rowMajorOrder */));

            result.reset();
            result.resize(vec3Tmp0, 0);

            FloatUtil.mapObjToWinCoords(getMinX(), getMaxY(), objZ, mat4PMv, view, 0, vec3Tmp0, 0, vec4Tmp1, vec4Tmp2);
            // System.err.printf("AABBox.mapToWindow.p2: %f, %f, %f -> %f, %f, %f%n", getMinX(), getMaxY(), objZ, vec3Tmp0[0], vec3Tmp0[1], vec3Tmp0[2]);
            result.resize(vec3Tmp0, 0);

            FloatUtil.mapObjToWinCoords(getMaxX(), getMinY(), objZ, mat4PMv, view, 0, vec3Tmp0, 0, vec4Tmp1, vec4Tmp2);
            // System.err.printf("AABBox.mapToWindow.p3: %f, %f, %f -> %f, %f, %f%n", getMaxX(), getMinY(), objZ, vec3Tmp0[0], vec3Tmp0[1], vec3Tmp0[2]);
            result.resize(vec3Tmp0, 0);

            FloatUtil.mapObjToWinCoords(getMaxX(), getMaxY(), objZ, mat4PMv, view, 0, vec3Tmp0, 0, vec4Tmp1, vec4Tmp2);
            // System.err.printf("AABBox.mapToWindow.p4: %f, %f, %f -> %f, %f, %f%n", getMaxX(), getMaxY(), objZ, vec3Tmp0[0], vec3Tmp0[1], vec3Tmp0[2]);
            result.resize(vec3Tmp0, 0);
        }

        if( !useCenterZ ) {
            final float objZ = getMaxZ();
            FloatUtil.mapObjToWinCoords(getMinX(), getMinY(), objZ, mat4PMv, view, 0, vec3Tmp0, 0, vec4Tmp1, vec4Tmp2);
            result.resize(vec3Tmp0, 0);

            FloatUtil.mapObjToWinCoords(getMinX(), getMaxY(), objZ, mat4PMv, view, 0, vec3Tmp0, 0, vec4Tmp1, vec4Tmp2);
            result.resize(vec3Tmp0, 0);

            FloatUtil.mapObjToWinCoords(getMaxX(), getMinY(), objZ, mat4PMv, view, 0, vec3Tmp0, 0, vec4Tmp1, vec4Tmp2);
            result.resize(vec3Tmp0, 0);

            FloatUtil.mapObjToWinCoords(getMaxX(), getMaxY(), objZ, mat4PMv, view, 0, vec3Tmp0, 0, vec4Tmp1, vec4Tmp2);
            result.resize(vec3Tmp0, 0);
        }
        if( DEBUG ) {
            System.err.printf("AABBox.mapToWindow: view[%d, %d], this %s -> %s%n", view[0], view[1], toString(), result.toString());
        }
        return result;
    }

    @Override
    public final String toString() {
        return "[ dim "+getWidth()+" x "+getHeight()+" x "+getDepth()+
               ", box "+low[0]+" / "+low[1]+" / "+low[2]+" .. "+high[0]+" / "+high[1]+" / "+high[2]+
               ", ctr "+center[0]+" / "+center[1]+" / "+center[2]+" ]";
    }
}
