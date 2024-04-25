/**
 * Copyright 2010-2024 JogAmp Community. All rights reserved.
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
package com.jogamp.math.geom;

import com.jogamp.math.FloatUtil;
import com.jogamp.math.Matrix4f;
import com.jogamp.math.Quaternion;
import com.jogamp.math.Ray;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec3f;
import com.jogamp.math.geom.plane.AffineTransform;


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
public final class AABBox {
    private static final boolean DEBUG = FloatUtil.DEBUG;
    /** Low left-bottom-far (xyz) coordinate */
    private final Vec3f lo = new Vec3f();
    /** High right-top-near (xyz) coordinate */
    private final Vec3f hi = new Vec3f();
    /** Computed center of {@link #lo} and {@link #hi}. */
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
     * Create a AABBox defining the low and high
     * @param low min xyz-coordinates
     * @param high max xyz-coordinates
     */
    public AABBox(final Vec3f low, final Vec3f high) {
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

    /** Returns the maximum right-top-near (xyz) coordinate */
    public final Vec3f getHigh() {
        return hi;
    }

    private final void setHigh(final float hx, final float hy, final float hz) {
        this.hi.set(hx, hy, hz);
    }

    /** Returns the minimum left-bottom-far (xyz) coordinate */
    public final Vec3f getLow() {
        return lo;
    }

    private final void setLow(final float lx, final float ly, final float lz) {
        this.lo.set(lx, ly, lz);
    }

    private final void computeCenter() {
        center.set(hi).add(lo).scale(1f/2f);
    }

    /**
     * Copy given AABBox 'src' values to this AABBox.
     *
     * @param src source AABBox
     * @return this AABBox for chaining
     */
    public final AABBox copy(final AABBox src) {
        lo.set(src.lo);
        hi.set(src.hi);
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
        this.lo.set(lx, ly, lz);
        this.hi.set(hx, hy, hz);
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
        this.lo.set(low);
        this.hi.set(high);
        computeCenter();
        return this;
    }

    /**
     * Resize width of this AABBox with explicit left- and right delta values
     * @param deltaLeft positive value will expand width, otherwise shrink width
     * @param deltaRight positive value will expand width, otherwise shrink width
     * @return this AABBox for chaining
     */
    public final AABBox resizeWidth(final float deltaLeft, final float deltaRight) {
        boolean mod = false;
        if( !FloatUtil.isZero(deltaLeft) ) {
            lo.setX( lo.x() - deltaLeft );
            mod = true;
        }
        if( !FloatUtil.isZero(deltaRight) ) {
            hi.setX( hi.x() + deltaRight );
            mod = true;
        }
        if( mod ) {
            computeCenter();
        }
        return this;
    }

    /**
     * Resize height of this AABBox with explicit bottom- and top delta values
     * @param deltaBottom positive value will expand height, otherwise shrink height
     * @param deltaTop positive value will expand height, otherwise shrink height
     * @return this AABBox for chaining
     */
    public final AABBox resizeHeight(final float deltaBottom, final float deltaTop) {
        boolean mod = false;
        if( !FloatUtil.isZero(deltaBottom) ) {
            lo.setY( lo.y() - deltaBottom );
            mod = true;
        }
        if( !FloatUtil.isZero(deltaTop) ) {
            hi.setY( hi.y() + deltaTop );
            mod = true;
        }
        if( mod ) {
            computeCenter();
        }
        return this;
    }

    /**
     * Assign values of given AABBox to this instance.
     *
     * @param o source AABBox
     * @return this AABBox for chaining
     */
    public final AABBox set(final AABBox o) {
        this.lo.set(o.lo);
        this.hi.set(o.hi);
        this.center.set(o.center);
        return this;
    }

    /**
     * Resize the AABBox to encapsulate another AABox
     * @param newBox AABBox to be encapsulated in
     * @return this AABBox for chaining
     */
    public final AABBox resize(final AABBox newBox) {
        final Vec3f newBL = newBox.getLow();
        final Vec3f newTR = newBox.getHigh();

        /** test low */
        if (newBL.x() < lo.x()) {
            lo.setX( newBL.x() );
        }
        if (newBL.y() < lo.y()) {
            lo.setY( newBL.y() );
        }
        if (newBL.z() < lo.z()) {
            lo.setZ( newBL.z() );
        }

        /** test high */
        if (newTR.x() > hi.x()) {
            hi.setX( newTR.x() );
        }
        if (newTR.y() > hi.y()) {
            hi.setY( newTR.y() );
        }
        if (newTR.z() > hi.z()) {
            hi.setZ( newTR.z() );
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
            final Vec3f newBL = t.transform(newBox.getLow(), tmpV3);
            if (newBL.x() < lo.x())
                lo.setX( newBL.x() );
            if (newBL.y() < lo.y())
                lo.setY( newBL.y() );
            if (newBL.z() < lo.z())
                lo.setZ( newBL.z() );
        }

        /** test high */
        {
            final Vec3f newTR = t.transform(newBox.getHigh(), tmpV3);
            if (newTR.x() > hi.x())
                hi.setX( newTR.x() );
            if (newTR.y() > hi.y())
                hi.setY( newTR.y() );
            if (newTR.z() > hi.z())
                hi.setZ( newTR.z() );
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
        if (x < lo.x()) {
            lo.setX( x );
        }
        if (y < lo.y()) {
            lo.setY( y );
        }
        if (z < lo.z()) {
            lo.setZ( z );
        }

        /** test high */
        if (x > hi.x()) {
            hi.setX( x );
        }
        if (y > hi.y()) {
            hi.setY( y );
        }
        if (z > hi.z()) {
            hi.setZ( z );
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
     * Returns whether this AABBox contains given 2D point.
     * @param x  x-axis coordinate value
     * @param y  y-axis coordinate value
     */
    public final boolean contains(final float x, final float y) {
        return !( x<lo.x() || x>hi.x() ||
                  y<lo.y() || y>hi.y() );
    }

    /**
     * Returns whether this AABBox contains given 3D point.
     * @param x x-axis coordinate value
     * @param y y-axis coordinate value
     * @param z z-axis coordinate value
     */
    public final boolean contains(final float x, final float y, final float z) {
        return !( x<lo.x() || x>hi.x() ||
                  y<lo.y() || y>hi.y() ||
                  z<lo.z() || z>hi.z() );
    }

    /** Returns whether this AABBox intersects (partially contains) given AABBox. */
    public final boolean intersects(final AABBox o) {
        return !( hi.x() < o.lo.x() ||
                  hi.y() < o.lo.y() ||
                  hi.z() < o.lo.z() ||
                  lo.x() > o.hi.x() ||
                  lo.y() > o.hi.y() ||
                  lo.z() > o.hi.z());
    }

    /** Returns whether this AABBox fully contains given AABBox. */
    public final boolean contains(final AABBox o) {
        return hi.x() >= o.hi.x() &&
               hi.y() >= o.hi.y() &&
               hi.z() >= o.hi.z() &&
               lo.x() <= o.lo.x() &&
               lo.y() <= o.lo.y() &&
               lo.z() <= o.lo.z();
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
        return (x >= x0 &&
                y >= y0 &&
                x + w <= x0 + _w &&
                y + h <= y0 + _h);
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
        final float extX  = hi.x() - center.x();
        if( Math.abs(diffX) > extX && diffX*dirX >= 0f ) return false;

        final float dirY  = ray.dir.y();
        final float diffY = ray.orig.y() - center.y();
        final float extY  = hi.y() - center.y();
        if( Math.abs(diffY) > extY && diffY*dirY >= 0f ) return false;

        final float dirZ  = ray.dir.z();
        final float diffZ = ray.orig.z() - center.z();
        final float extZ  = hi.z() - center.z();
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

        /**
         * Use unrolled version below...
         *
         * Find candidate planes.
            for(int i=0; i<3; i++) {
                final float origin_i = origin.get(i);
                final float dir_i = dir.get(i);
                final float bl_i = bl.get(i);
                final float tr_i = tr.get(i);
                if(origin_i < bl_i) {
                    result.set(i, bl_i);
                    inside    = false;

                    // Calculate T distances to candidate planes
                    if( 0 != Float.floatToIntBits(dir_i) ) {
                        maxT[i] = (bl_i - origin_i) / dir_i;
                    }
                } else if(origin_i > tr_i) {
                    result.set(i, tr_i);
                    inside    = false;

                    // Calculate T distances to candidate planes
                    if( 0 != Float.floatToIntBits(dir_i) ) {
                        maxT[i] = (tr_i - origin_i) / dir_i;
                    }
                }
            }
        */
        // Find candidate planes, unrolled
        {
            if(origin.x() < lo.x()) {
                result.setX(lo.x());
                inside    = false;

                // Calculate T distances to candidate planes
                if( 0 != Float.floatToIntBits(dir.x()) ) {
                    maxT[0] = (lo.x() - origin.x()) / dir.x();
                }
            } else if(origin.x() > hi.x()) {
                result.setX(hi.x());
                inside    = false;

                // Calculate T distances to candidate planes
                if( 0 != Float.floatToIntBits(dir.x()) ) {
                    maxT[0] = (hi.x() - origin.x()) / dir.x();
                }
            }
        }
        {
            if(origin.y() < lo.y()) {
                result.setY(lo.y());
                inside    = false;

                // Calculate T distances to candidate planes
                if( 0 != Float.floatToIntBits(dir.y()) ) {
                    maxT[1] = (lo.y() - origin.y()) / dir.y();
                }
            } else if(origin.y() > hi.y()) {
                result.setY(hi.y());
                inside    = false;

                // Calculate T distances to candidate planes
                if( 0 != Float.floatToIntBits(dir.y()) ) {
                    maxT[1] = (hi.y() - origin.y()) / dir.y();
                }
            }
        }
        {
            if(origin.z() < lo.z()) {
                result.setZ(lo.z());
                inside    = false;

                // Calculate T distances to candidate planes
                if( 0 != Float.floatToIntBits(dir.z()) ) {
                    maxT[2] = (lo.z() - origin.z()) / dir.z();
                }
            } else if(origin.z() > hi.z()) {
                result.setZ(hi.z());
                inside    = false;

                // Calculate T distances to candidate planes
                if( 0 != Float.floatToIntBits(dir.z()) ) {
                    maxT[2] = (hi.z() - origin.z()) / dir.z();
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
            if( 0 != ( Float.floatToIntBits(maxT[whichPlane]) & FloatUtil.IEC559_SIGN_BIT ) ) {
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
                    if(result.y() < lo.y() - epsilon || result.y() > hi.y() + epsilon) { return null; }
                    result.setZ( origin.z() + maxT[whichPlane] * dir.z() );
                    if(result.z() < lo.z() - epsilon || result.z() > hi.z() + epsilon) { return null; }
                    break;
                case 1:
                    result.setX( origin.x() + maxT[whichPlane] * dir.x() );
                    if(result.x() < lo.x() - epsilon || result.x() > hi.x() + epsilon) { return null; }
                    result.setZ( origin.z() + maxT[whichPlane] * dir.z() );
                    if(result.z() < lo.z() - epsilon || result.z() > hi.z() + epsilon) { return null; }
                    break;
                case 2:
                    result.setX( origin.x() + maxT[whichPlane] * dir.x() );
                    if(result.x() < lo.x() - epsilon || result.x() > hi.x() + epsilon) { return null; }
                    result.setY( origin.y() + maxT[whichPlane] * dir.y() );
                    if(result.y() < lo.y() - epsilon || result.y() > hi.y() + epsilon) { return null; }
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
        return lo.dist(hi);
    }

    /** Returns computed center of this AABBox of {@link #getLow()} and {@link #getHigh()}. */
    public final Vec3f getCenter() {
        return center;
    }

    /**
     * Scale this AABBox by a constant around fixed center
     * <p>
     * high and low is recomputed by scaling its distance to fixed center.
     * </p>
     * @param s scale factor
     * @return this AABBox for chaining
     * @see #scale2(float, float[])
     */
    public final AABBox scale(final float s) {
        final Vec3f tmp = new Vec3f();
        tmp.set(hi).sub(center).scale(s);
        hi.set(center).add(tmp);

        tmp.set(lo).sub(center).scale(s);
        lo.set(center).add(tmp);

        return this;
    }
    /**
     * Scale this AABBox by constants around fixed center
     * <p>
     * high and low is recomputed by scaling its distance to fixed center.
     * </p>
     * @param sX horizontal scale factor
     * @param sY vertical scale factor
     * @param sZ Z-axis scale factor
     * @return this AABBox for chaining
     * @see #scale2(float, float[])
     */
    public final AABBox scale(final float sX, final float sY, final float sZ) {
        final Vec3f tmp = new Vec3f();
        tmp.set(hi).sub(center).mul(sX, sY, sZ);
        hi.set(center).add(tmp);

        tmp.set(lo).sub(center).mul(sX, sY, sZ);
        lo.set(center).add(tmp);

        return this;
    }

    /**
     * Scale this AABBox by a constant, recomputing center
     * <p>
     * high and low is scaled and center recomputed.
     * </p>
     * @param s scale factor
     * @return this AABBox for chaining
     * @see #scale(float, float[])
     */
    public final AABBox scale2(final float s) {
        hi.scale(s);
        lo.scale(s);
        computeCenter();
        return this;
    }

    /**
     * Scale this AABBox by constants, recomputing center
     * <p>
     * high and low is scaled and center recomputed.
     * </p>
     * @param sX horizontal scale factor
     * @param sY vertical scale factor
     * @param sZ Z-axis scale factor
     * @return this AABBox for chaining
     * @see #scale(float, float[])
     */
    public final AABBox scale2(final float sX, final float sY, final float sZ) {
        hi.mul(sX, sY, sZ);
        lo.mul(sX, sY, sZ);
        computeCenter();
        return this;
    }

    /**
     * Translate this AABBox by a float[3] vector
     * @param dx the translation x-component
     * @param dy the translation y-component
     * @param dz the translation z-component
     * @param t the float[3] translation vector
     * @return this AABBox for chaining
     */
    public final AABBox translate(final float dx, final float dy, final float dz) {
        lo.add(dx, dy, dz);
        hi.add(dx, dy, dz);
        computeCenter();
        return this;
    }

    /**
     * Translate this AABBox by a float[3] vector
     * @param t the float[3] translation vector
     * @return this AABBox for chaining
     */
    public final AABBox translate(final Vec3f t) {
        lo.add(t);
        hi.add(t);
        computeCenter();
        return this;
    }

    /**
     * Rotate this AABBox by a float[3] vector
     * @param quat the {@link Quaternion} used for rotation
     * @return this AABBox for chaining
     */
    public final AABBox rotate(final Quaternion quat) {
        quat.rotateVector(lo, lo);
        quat.rotateVector(hi, hi);
        computeCenter();
        return this;
    }

    public final float getMinX() {
        return lo.x();
    }

    public final float getMinY() {
        return lo.y();
    }

    public final float getMinZ() {
        return lo.z();
    }

    public final float getMaxX() {
        return hi.x();
    }

    public final float getMaxY() {
        return hi.y();
    }

    public final float getMaxZ() {
        return hi.z();
    }

    public final float getWidth(){
        return hi.x() - lo.x();
    }

    public final float getHeight() {
        return hi.y() - lo.y();
    }

    public final float getDepth() {
        return hi.z() - lo.z();
    }

    /** Returns the volume, i.e. width * height * depth */
    public final float getVolume() {
        return getWidth() * getHeight() * getDepth();
    }

    /** Return true if {@link #getVolume()} is {@link FloatUtil#isZero(float)}, considering epsilon. */
    public final boolean hasZeroVolume() {
        return FloatUtil.isZero(getVolume());
    }

    /** Returns the assumed 2D area, i.e. width * height while assuming low and high lies on same plane. */
    public final float get2DArea() {
        return getWidth() * getHeight();
    }

    /** Return true if {@link #get2DArea()} is {@link FloatUtil#isZero(float)}, considering epsilon. */
    public final boolean hasZero2DArea() {
        return FloatUtil.isZero(get2DArea());
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
        return lo.isEqual(other.lo) && hi.isEqual(other.hi);
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
        out.resize( mat.mulVec3f(lo, tmp) );
        out.resize( mat.mulVec3f(hi, tmp) );
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
        return "[dim "+getWidth()+" x "+getHeight()+" x "+getDepth()+
               ", box "+lo+" .. "+hi+", ctr "+center+"]";
    }
}
