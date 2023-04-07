/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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

import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.Ray;
import com.jogamp.opengl.math.Recti;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.util.PMVMatrix;


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
    private final Vec3f low = new Vec3f();
    private final Vec3f high = new Vec3f();
    private final Vec3f center = new Vec3f();

    /**
     * Create an Axis Aligned bounding box (AABBox) with the
     * inverse low/high, allowing the next {@link #resize(float, float, float)} command to hit.
     * <p>
     * The dimension, i.e. {@link #getWidth()} abd {@link #getHeight()} is {@link Float#isInfinite()} thereafter.
     * </p>
     * @see #reset()
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
     * Resets this box to the inverse low/high, allowing the next {@link #resize(float, float, float)} command to hit.
     * <p>
     * The dimension, i.e. {@link #getWidth()} abd {@link #getHeight()} is {@link Float#isInfinite()} thereafter.
     * </p>
     * @return this AABBox for chaining
     */
    public final AABBox reset() {
        setLow(Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE);
        setHigh(-1*Float.MAX_VALUE,-1*Float.MAX_VALUE,-1*Float.MAX_VALUE);
        center.set( 0f, 0f, 0f);
        return this;
    }

    /** Get the max xyz-coordinates
     * @return max xyz coordinates
     */
    public final Vec3f getHigh() {
        return high;
    }

    private final void setHigh(final float hx, final float hy, final float hz) {
        this.high.set(hx, hy, hz);
    }

    /** Get the min xyz-coordinates
     * @return min xyz coordinates
     */
    public final Vec3f getLow() {
        return low;
    }

    private final void setLow(final float lx, final float ly, final float lz) {
        this.low.set(lx, ly, lz);
    }

    private final void computeCenter() {
        center.set(high).add(low).scale(1f/2f);
    }

    /**
     * Copy given AABBox 'src' values to this AABBox.
     *
     * @param src source AABBox
     * @return this AABBox for chaining
     */
    public final AABBox copy(final AABBox src) {
        low.set(src.low);
        high.set(src.high);
        center.set(src.center);
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
        this.low.set(lx, ly, lz);
        this.high.set(hx, hy, hz);
        computeCenter();
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
    public final AABBox setSize(final Vec3f low, final Vec3f high) {
        this.low.set(low);
        this.high.set(high);
        computeCenter();
        return this;
    }

    /**
     * Resize the AABBox to encapsulate another AABox
     * @param newBox AABBox to be encapsulated in
     * @return this AABBox for chaining
     */
    public final AABBox resize(final AABBox newBox) {
        final Vec3f newLow = newBox.getLow();
        final Vec3f newHigh = newBox.getHigh();

        /** test low */
        if (newLow.x() < low.x()) {
            low.setX( newLow.x() );
        }
        if (newLow.y() < low.y()) {
            low.setY( newLow.y() );
        }
        if (newLow.z() < low.z()) {
            low.setZ( newLow.z() );
        }

        /** test high */
        if (newHigh.x() > high.x()) {
            high.setX( newHigh.x() );
        }
        if (newHigh.y() > high.y()) {
            high.setY( newHigh.y() );
        }
        if (newHigh.z() > high.z()) {
            high.setZ( newHigh.z() );
        }
        computeCenter();
        return this;
    }

    /**
     * Resize the AABBox to encapsulate another AABox, which will be <i>transformed</i> on the fly first.
     * @param newBox AABBox to be encapsulated in
     * @param t the {@link AffineTransform} applied on <i>newBox</i> on the fly
     * @param tmpV3 temporary storage
     * @return this AABBox for chaining
     */
    public final AABBox resize(final AABBox newBox, final AffineTransform t, final Vec3f tmpV3) {
        /** test low */
        {
            final Vec3f newBoxLow = newBox.getLow();
            t.transform(newBoxLow, tmpV3);
            if (tmpV3.x() < low.x())
                low.setX( tmpV3.x() );
            if (tmpV3.y() < low.y())
                low.setY( tmpV3.y() );
            if (tmpV3.z() < low.z())
                low.setZ( tmpV3.z() );
        }

        /** test high */
        {
            final Vec3f newBoxHigh = newBox.getHigh();
            t.transform(newBoxHigh, tmpV3);
            if (tmpV3.x() > high.x())
                high.setX( tmpV3.x() );
            if (tmpV3.y() > high.y())
                high.setY( tmpV3.y() );
            if (tmpV3.z() > high.z())
                high.setZ( tmpV3.z() );
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
        if (x < low.x()) {
            low.setX( x );
        }
        if (y < low.y()) {
            low.setY( y );
        }
        if (z < low.z()) {
            low.setZ( z );
        }

        /** test high */
        if (x > high.x()) {
            high.setX( x );
        }
        if (y > high.y()) {
            high.setY( y );
        }
        if (z > high.z()) {
            high.setZ( z );
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
     * Resize the AABBox to encapsulate the passed
     * xyz-coordinates.
     * @param xyz xyz-axis coordinate values
     * @return this AABBox for chaining
     */
    public final AABBox resize(final Vec3f xyz) {
        return resize(xyz.x(), xyz.y(), xyz.z());
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
        if(x<low.x() || x>high.x()){
            return false;
        }
        if(y<low.y()|| y>high.y()){
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
        if(x<low.x() || x>high.x()){
            return false;
        }
        if(y<low.y()|| y>high.y()){
            return false;
        }
        if(z<low.z() || z>high.z()){
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

        final float dirX  = ray.dir.x();
        final float diffX = ray.orig.x() - center.x();
        final float extX  = high.x() - center.x();
        if( Math.abs(diffX) > extX && diffX*dirX >= 0f ) return false;

        final float dirY  = ray.dir.y();
        final float diffY = ray.orig.y() - center.y();
        final float extY  = high.y() - center.y();
        if( Math.abs(diffY) > extY && diffY*dirY >= 0f ) return false;

        final float dirZ  = ray.dir.z();
        final float diffZ = ray.orig.z() - center.z();
        final float extZ  = high.z() - center.z();
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
     * @return float[3] result of intersection coordinates, or null if none exists
     */
    public final Vec3f getRayIntersection(final Vec3f result, final Ray ray, final float epsilon,
                                          final boolean assumeIntersection) {
        final float[] maxT = { -1f, -1f, -1f };

        final Vec3f origin = ray.orig;
        final Vec3f dir = ray.dir;

        boolean inside = true;

        // Find candidate planes.
        for(int i=0; i<3; i++) {
            final float origin_i = origin.get(i);
            final float dir_i = dir.get(i);
            final float low_i = low.get(i);
            final float high_i = high.get(i);
            if(origin_i < low_i) {
                result.set(i, low_i);
                inside    = false;

                // Calculate T distances to candidate planes
                if( 0 != Float.floatToIntBits(dir_i) ) {
                    maxT[i] = (low_i - origin_i) / dir_i;
                }
            } else if(origin_i > high_i) {
                result.set(i, high_i);
                inside    = false;

                // Calculate T distances to candidate planes
                if( 0 != Float.floatToIntBits(dir_i) ) {
                    maxT[i] = (high_i - origin_i) / dir_i;
                }
            }
        }

        // Ray origin inside bounding box
        if(inside) {
            result.set(origin);
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
                    result.setY( origin.y() + maxT[whichPlane] * dir.y() );
                    if(result.y() < low.y() - epsilon || result.y() > high.y() + epsilon) { return null; }
                    result.setZ( origin.z() + maxT[whichPlane] * dir.z() );
                    if(result.z() < low.z() - epsilon || result.z() > high.z() + epsilon) { return null; }
                    break;
                case 1:
                    result.setX( origin.x() + maxT[whichPlane] * dir.x() );
                    if(result.x() < low.x() - epsilon || result.x() > high.x() + epsilon) { return null; }
                    result.setZ( origin.z() + maxT[whichPlane] * dir.z() );
                    if(result.z() < low.z() - epsilon || result.z() > high.z() + epsilon) { return null; }
                    break;
                case 2:
                    result.setX( origin.x() + maxT[whichPlane] * dir.x() );
                    if(result.x() < low.x() - epsilon || result.x() > high.x() + epsilon) { return null; }
                    result.setY( origin.y() + maxT[whichPlane] * dir.y() );
                    if(result.y() < low.y() - epsilon || result.y() > high.y() + epsilon) { return null; }
                    break;
                default:
                    throw new InternalError("XXX");
            }
        } else {
            switch( whichPlane ) {
                case 0:
                    result.setY( origin.y() + maxT[whichPlane] * dir.y() );
                    result.setZ( origin.z() + maxT[whichPlane] * dir.z() );
                    break;
                case 1:
                    result.setX( origin.x() + maxT[whichPlane] * dir.x() );
                    result.setZ( origin.z() + maxT[whichPlane] * dir.z() );
                    break;
                case 2:
                    result.setX( origin.x() + maxT[whichPlane] * dir.x() );
                    result.setY( origin.y() + maxT[whichPlane] * dir.y() );
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
        return low.dist(high);
    }

    /**
     * Get the Center of this AABBox
     * @return the xyz-coordinates of the center of the AABBox
     */
    public final Vec3f getCenter() {
        return center;
    }

    /**
     * Scale this AABBox by a constant around fixed center
     * <p>
     * high and low is recomputed by scaling its distance to fixed center.
     * </p>
     * @param size a constant float value
     * @return this AABBox for chaining
     * @see #scale2(float, float[])
     */
    public final AABBox scale(final float size) {
        final Vec3f tmp = new Vec3f();
        tmp.set(high).sub(center).scale(size);
        high.set(center).add(tmp);

        tmp.set(low).sub(center).scale(size);
        low.set(center).add(tmp);

        return this;
    }

    /**
     * Scale this AABBox by a constant, recomputing center
     * <p>
     * high and low is scaled and center recomputed.
     * </p>
     * @param size a constant float value
     * @return this AABBox for chaining
     * @see #scale(float, float[])
     */
    public final AABBox scale2(final float size) {
        high.scale(size);
        low.scale(size);
        computeCenter();
        return this;
    }

    /**
     * Translate this AABBox by a float[3] vector
     * @param t the float[3] translation vector
     * @return this AABBox for chaining
     */
    public final AABBox translate(final Vec3f t) {
        low.add(t);
        high.add(t);
        computeCenter();
        return this;
    }

    /**
     * Rotate this AABBox by a float[3] vector
     * @param quat the {@link Quaternion} used for rotation
     * @return this AABBox for chaining
     */
    public final AABBox rotate(final Quaternion quat) {
        quat.rotateVector(low, low);
        quat.rotateVector(high, high);
        computeCenter();
        return this;
    }

    public final float getMinX() {
        return low.x();
    }

    public final float getMinY() {
        return low.y();
    }

    public final float getMinZ() {
        return low.z();
    }

    public final float getMaxX() {
        return high.x();
    }

    public final float getMaxY() {
        return high.y();
    }

    public final float getMaxZ() {
        return high.z();
    }

    public final float getWidth(){
        return high.x() - low.x();
    }

    public final float getHeight() {
        return high.y() - low.y();
    }

    public final float getDepth() {
        return high.z() - low.z();
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
        return low.isEqual(other.low) && high.isEqual(other.high);
    }
    @Override
    public final int hashCode() {
        throw new InternalError("hashCode not designed");
    }

    /**
     * Transform this box using the given {@link Matrix4f} into {@code out}
     * @param mat transformation {@link Matrix4f}
     * @param out the resulting {@link AABBox}
     * @return the resulting {@link AABBox} for chaining
     */
    public AABBox transform(final Matrix4f mat, final AABBox out) {
        final Vec3f tmp = new Vec3f();
        out.reset();
        out.resize( mat.mulVec3f(low, tmp) );
        out.resize( mat.mulVec3f(high, tmp) );
        out.computeCenter();
        return out;
    }

    /**
     * Transform this box using the {@link PMVMatrix#getMvMat() modelview} of the given {@link PMVMatrix} into {@code out}
     * @param pmv transformation {@link PMVMatrix}
     * @param out the resulting {@link AABBox}
     * @return the resulting {@link AABBox} for chaining
     */
    public AABBox transformMv(final PMVMatrix pmv, final AABBox out) {
        final Vec3f tmp = new Vec3f();
        out.reset();
        out.resize( pmv.mulMvMatVec3f(low, tmp) );
        out.resize( pmv.mulMvMatVec3f(high, tmp) );
        out.computeCenter();
        return out;
    }

    /**
     * Assume this bounding box as being in object space and
     * compute the window bounding box.
     * <p>
     * If <code>useCenterZ</code> is <code>true</code>,
     * only 4 {@link FloatUtil#mapObjToWin(float, float, float, float[], int[], float[], float[], float[]) mapObjToWinCoords}
     * operations are made on points [1..4] using {@link #getCenter()}'s z-value.
     * Otherwise 8 {@link FloatUtil#mapObjToWin(float, float, float, float[], int[], float[], float[], float[]) mapObjToWinCoords}
     * operation on all 8 points are performed.
     * </p>
     * <pre>
     *  .z() ------ [4]
     *   |          |
     *   |          |
     *  .y() ------ [3]
     * </pre>
     * @param mat4PMv [projection] x [modelview] matrix, i.e. P x Mv
     * @param viewport viewport rectangle
     * @param useCenterZ
     * @param vec3Tmp0 3 component vector for temp storage
     * @param vec4Tmp1 4 component vector for temp storage
     * @param vec4Tmp2 4 component vector for temp storage
     * @return
     */
    public AABBox mapToWindow(final AABBox result, final Matrix4f mat4PMv, final Recti viewport, final boolean useCenterZ) {
        final Vec3f tmp = new Vec3f();
        final Vec3f winPos = new Vec3f();
        {
            final float objZ = useCenterZ ? center.z() : getMinZ();
            result.reset();

            Matrix4f.mapObjToWin(tmp.set(getMinX(), getMinY(), objZ), mat4PMv, viewport, winPos);
            result.resize(winPos);

            Matrix4f.mapObjToWin(tmp.set(getMinX(), getMaxY(), objZ), mat4PMv, viewport, winPos);
            result.resize(winPos);

            Matrix4f.mapObjToWin(tmp.set(getMaxX(), getMaxY(), objZ), mat4PMv, viewport, winPos);
            result.resize(winPos);

            Matrix4f.mapObjToWin(tmp.set(getMaxX(), getMinY(), objZ), mat4PMv, viewport, winPos);
            result.resize(winPos);
        }

        if( !useCenterZ ) {
            final float objZ = getMaxZ();

            Matrix4f.mapObjToWin(tmp.set(getMinX(), getMinY(), objZ), mat4PMv, viewport, winPos);
            result.resize(winPos);

            Matrix4f.mapObjToWin(tmp.set(getMinX(), getMaxY(), objZ), mat4PMv, viewport, winPos);
            result.resize(winPos);

            Matrix4f.mapObjToWin(tmp.set(getMaxX(), getMaxY(), objZ), mat4PMv, viewport, winPos);
            result.resize(winPos);

            Matrix4f.mapObjToWin(tmp.set(getMaxX(), getMinY(), objZ), mat4PMv, viewport, winPos);
            result.resize(winPos);
        }
        if( DEBUG ) {
            System.err.printf("AABBox.mapToWindow: view[%s], this %s -> %s%n", viewport, toString(), result.toString());
        }
        return result;
    }

    @Override
    public final String toString() {
        return "[ dim "+getWidth()+" x "+getHeight()+" x "+getDepth()+
               ", box "+low+" .. "+high+", ctr "+center+" ]";
    }
}
