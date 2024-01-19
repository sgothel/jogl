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

import com.jogamp.math.FovHVHalves;
import com.jogamp.math.Matrix4f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;

/**
 * Providing frustum {@link #getPlanes() planes} derived by different inputs
 * ({@link #updateFrustumPlanes(float[], int) P*MV}, ..) used to classify objects
 * <ul>
 *   <li> {@link #classifyPoint(Vec3f) point} </li>
 *   <li> {@link #classifySphere(Vec3f, float) sphere} </li>
 * </ul>
 * and to test whether they are outside
 * <ul>
 *   <li> {@link #isOutside(Vec3f) point} </li>
 *   <li> {@link #isSphereOutside(Vec3f, float) sphere} </li>
 *   <li> {@link #isOutside(AABBox) bounding-box} </li>
 *   <li> {@link #isOutside(Cube) cube} </li>
 * </ul>
 *
 * <p>
 * Extracting the world-frustum planes from the P*Mv:
 * <pre>
 * Fast Extraction of Viewing Frustum Planes from the World-View-Projection Matrix
 *   Gil Gribb <ggribb@ravensoft.com>
 *   Klaus Hartmann <k_hartmann@osnabrueck.netsurf.de>
 *   http://graphics.cs.ucf.edu/cap4720/fall2008/plane_extraction.pdf
 * </pre>
 * Classifying Point, Sphere and AABBox:
 * <pre>
 * Efficient View Frustum Culling
 *   Daniel Sýkora <sykorad@fel.cvut.cz>
 *   Josef Jelínek <jelinej1@fel.cvut.cz>
 *   http://www.cg.tuwien.ac.at/hostings/cescg/CESCG-2002/DSykoraJJelinek/index.html
 * </pre>
 * <pre>
 * Lighthouse3d.com
 * http://www.lighthouse3d.com/tutorials/view-frustum-culling/
 * </pre>
 *
 * Fundamentals about Planes, Half-Spaces and Frustum-Culling:<br/>
 * <pre>
 * Planes and Half-Spaces,  Max Wagner <mwagner@digipen.edu>
 * http://www.emeyex.com/site/tuts/PlanesHalfSpaces.pdf
 * </pre>
 * <pre>
 * Frustum Culling,  Max Wagner <mwagner@digipen.edu>
 * http://www.emeyex.com/site/tuts/FrustumCulling.pdf
 * </pre>
 * </p>
 */
public class Frustum {
    /**
     * {@link Frustum} description by {@link #fovhv} and {@link #zNear}, {@link #zFar}.
     */
    public static class FovDesc {
        /** Field of view in both directions, may not be centered, either {@link FovHVHalves#inTangents} or radians. */
        public final FovHVHalves fovhv;
        /** Near Z */
        public final float zNear;
        /** Far Z */
        public final float zFar;
        /**
         * @param fovhv field of view in both directions, may not be centered, either {@link FovHVHalves#inTangents} or radians
         * @param zNear
         * @param zFar
         * @throws IllegalArgumentException if {@code zNear <= 0} or {@code zFar <= zNear}.
         */
        public FovDesc(final FovHVHalves fovhv, final float zNear, final float zFar) throws IllegalArgumentException {
            if( zNear <= 0.0f || zFar <= zNear ) {
                throw new IllegalArgumentException("Requirements zNear > 0 and zFar > zNear, but zNear "+zNear+", zFar "+zFar);
            }
            this.fovhv = fovhv;
            this.zNear = zNear;
            this.zFar = zFar;
        }
        @Override
        public final String toString() {
            return "FrustumFovDesc["+fovhv.toStringInDegrees()+", Z["+zNear+" - "+zFar+"]]";
        }
    }

    /** Normalized planes[l, r, b, t, n, f] */
	protected final Plane[] planes = new Plane[6];

	/**
	 * Creates an undefined instance w/o calculating the frustum.
	 * <p>
	 * Use one of the <code>update(..)</code> methods to set the {@link #getPlanes() planes}.
	 * </p>
	 * @see #updateByPlanes(Plane[])
	 * @see #updateFrustumPlanes(float[], int)
	 */
    public Frustum() {
        planes[LEFT  ] = new Plane();
        planes[RIGHT ] = new Plane();
        planes[BOTTOM] = new Plane();
        planes[TOP   ] = new Plane();
        planes[NEAR  ] = new Plane();
        planes[FAR   ] = new Plane();
    }

    public Frustum(final Frustum o) {
        planes[LEFT  ] = new Plane(o.planes[LEFT]);
        planes[RIGHT ] = new Plane(o.planes[RIGHT]);
        planes[BOTTOM] = new Plane(o.planes[BOTTOM]);
        planes[TOP   ] = new Plane(o.planes[TOP]);
        planes[NEAR  ] = new Plane(o.planes[NEAR]);
        planes[FAR   ] = new Plane(o.planes[FAR]);
    }

    public Frustum set(final Frustum o) {
        planes[LEFT  ].set(o.planes[LEFT]);
        planes[RIGHT ].set(o.planes[RIGHT]);
        planes[BOTTOM].set(o.planes[BOTTOM]);
        planes[TOP   ].set(o.planes[TOP]);
        planes[NEAR  ].set(o.planes[NEAR]);
        planes[FAR   ].set(o.planes[FAR]);
        return this;
    }

	/**
	 * Plane equation := dot(n, x - p) = 0 ->  Ax + By + Cz + d == 0
	 * <p>
	 * In order to work w/ {@link Frustum#isOutside(AABBox) isOutside(..)} methods,
	 * the normals have to point to the inside of the frustum.
	 * </p>
	 */
    public static class Plane {
        /** Normal of the plane */
        public final Vec3f n;

        /** Distance to origin */
        public float d;

        public Plane() {
            n = new Vec3f();
            d = 0f;
        }

        public Plane(final Plane o) {
            n = new Vec3f(o.n);
            d = o.d;
        }

        public Plane set(final Plane o) {
            n.set(o.n);
            d = o.d;
            return this;
        }

        /**
         * Setup of plane using 3 points. None of the three points are mutated.
         * <p>
         * Since this method may not properly define whether the normal points inside the frustum,
         * consider using {@link #set(Vec3f, Vec3f)}.
         * </p>
         * @param p0 point on plane, used as the shared start-point for vec(p0->p1) and vec(p0->p2)
         * @param p1 point on plane
         * @param p2 point on plane
         * @return this plane for chaining
         */
        public Plane set(final Vec3f p0, final Vec3f p1, final Vec3f p2) {
            final Vec3f v = p1.minus(p0);
            final Vec3f u = p2.minus(p0);
            n.cross(v, u).normalize();
            d = n.copy().scale(-1).dot(p0);
            return this;
        }

        /**
         * Setup of plane using given normal and one point on plane. The given normal is mutated, the point not mutated.
         * @param n normal to plane pointing to the inside of this frustum
         * @param p0 point on plane, consider choosing the closest point to origin
         * @return this plane for chaining
         */
        public Plane set(final Vec3f n, final Vec3f p0) {
            this.n.set(n);
            d = n.scale(-1).dot(p0);
            return this;
        }

        /** Sets the given {@link Vec4f} {@code out} to {@code ( n, d )}. Returns {@code out} for chaining. */
        public Vec4f toVec4f(final Vec4f out) {
            out.set(n, d);
            return out;
        }

        /**
         * Sets the given {@code [float[off]..float[off+4])} {@code out} to {@code ( n, d )}.
         * @param out the {@code float[off+4]} output array
         */
        public void toFloats(final float[/* off+4] */] out, final int off) {
            out[off+0] = n.x();
            out[off+1] = n.y();
            out[off+2] = n.z();
            out[off+3] = d;
        }

        /**
         * Return signed distance of plane to given point.
         * <ul>
         *   <li>If dist &lt; 0 , then the point p lies in the negative halfspace.</li>
         *   <li>If dist = 0 , then the point p lies in the plane.</li>
         *   <li>If dist &gt; 0 , then the point p lies in the positive halfspace.</li>
         * </ul>
         * A plane cuts 3D space into 2 half spaces.
         * <p>
         * Positive halfspace is where the plane’s normals vector points into.
         * </p>
         * <p>
         * Negative halfspace is the <i>other side</i> of the plane, i.e. *-1
         * </p>
         **/
        public final float distanceTo(final float x, final float y, final float z) {
            return n.x() * x + n.y() * y + n.z() * z + d;
        }

        /** Return distance of plane to given point, see {@link #distanceTo(float, float, float)}. */
        public final float distanceTo(final Vec3f p) {
            return n.dot(p) + d;
        }

        @Override
        public String toString() {
            return "Plane[ [ " + n + " ], " + d + "]";
        }
    }

    /** Index for left plane: {@value} */
    public static final int LEFT   = 0;
    /** Index for right plane: {@value} */
    public static final int RIGHT  = 1;
    /** Index for bottom plane: {@value} */
    public static final int BOTTOM = 2;
    /** Index for top plane: {@value} */
    public static final int TOP    = 3;
    /** Index for near plane: {@value} */
    public static final int NEAR   = 4;
    /** Index for far plane: {@value} */
    public static final int FAR    = 5;

    /**
     * {@link Plane}s are ordered in the returned array as follows:
     * <ul>
     *   <li>{@link #LEFT}</li>
     *   <li>{@link #RIGHT}</li>
     *   <li>{@link #BOTTOM}</li>
     *   <li>{@link #TOP}</li>
     *   <li>{@link #NEAR}</li>
     *   <li>{@link #FAR}</li>
     * </ul>
     * <p>
     * {@link Plane}'s normals are pointing to the inside of the frustum
     * in order to work w/ {@link #isOutside(AABBox) isOutside(..)} methods.
     * </p>
     *
     * @return array of normalized {@link Plane}s, order see above.
     */
    public final Plane[] getPlanes() { return planes; }

    /**
     * Sets each of the given {@link Vec4f}[6] {@code out} to {@link Plane#toVec4f(Vec4f)}
     * in the order {@link #LEFT}, {@link #RIGHT}, {@link #BOTTOM}, {@link #TOP}, {@link #NEAR}, {@link #FAR}.
     * @param out the {@link Vec4f}[6] output array
     * @return {@code out} for chaining
     */
    public Vec4f[] getPlanes(final Vec4f[] out) {
        planes[LEFT  ].toVec4f(out[0]);
        planes[RIGHT ].toVec4f(out[1]);
        planes[BOTTOM].toVec4f(out[2]);
        planes[TOP   ].toVec4f(out[3]);
        planes[NEAR  ].toVec4f(out[4]);
        planes[FAR   ].toVec4f(out[5]);
        return out;
    }

    /** Sets the given {@code [float[off]..float[off+4*6])} {@code out} to {@code ( n, d )}. */
    /**
     * Sets each of the given {@code [float[off]..float[off+4*6])} {@code out} to {@link Plane#toFloats(float[], int)},
     * i.e. [n.x, n.y, n.z, d, ...].
     * <p>
     * Plane order is as follows: {@link #LEFT}, {@link #RIGHT}, {@link #BOTTOM}, {@link #TOP}, {@link #NEAR}, {@link #FAR}.
     * </p>
     * @param out the {@code float[off+4*6]} output array
     * @return {@code out} for chaining
     */
    public void getPlanes(final float[/* off+4*6] */] out, final int off) {
        planes[LEFT  ].toFloats(out, off+4*0);
        planes[RIGHT ].toFloats(out, off+4*1);
        planes[BOTTOM].toFloats(out, off+4*2);
        planes[TOP   ].toFloats(out, off+4*3);
        planes[NEAR  ].toFloats(out, off+4*4);
        planes[FAR   ].toFloats(out, off+4*5);
    }

    /**
     * Copy the given <code>src</code> planes into this this instance's planes.
     * @param src the 6 source planes
     */
    public final void updateByPlanes(final Plane[] src) {
        for (int i = 0; i < 6; ++i) {
            final Plane pD = planes[i];
            final Plane pS = src[i];
            pD.d = pS.d;
            System.arraycopy(pS.n, 0, pD.n, 0, 3);
        }
    }

    /**
     * Calculate the frustum planes in world coordinates
     * using the passed {@link FovDesc}.
     * <p>
     * Operation Details:
     * <ul>
     *   <li>The given {@link FovDesc} will be transformed
     *       into the given perspective matrix (column major order) first,
     *       see {@link Matrix4f#setToPerspective(FovHVHalves, float, float)}.</li>
     *   <li>Then the perspective matrix is used to {@link Matrix4f#updateFrustumPlanes(Frustum)} this instance.</li>
     * </ul>
     * </p>
     * <p>
     * Frustum plane's normals will point to the inside of the viewing frustum,
     * as required by this class.
     * </p>
     *
     * @param m 4x4 matrix in column-major order (also result)
     * @param fovDesc {@link Frustum} {@link FovDesc}
     * @return given matrix for chaining
     * @see Matrix4f#setToPerspective(FovHVHalves, float, float)
     * @see Matrix4f#updateFrustumPlanes(Frustum)
     * @see Matrix4f#getFrustum(Frustum, FovDesc)
     */
    public Matrix4f updateByFovDesc(final Matrix4f m, final FovDesc fovDesc) {
        m.setToPerspective(fovDesc.fovhv, fovDesc.zNear, fovDesc.zFar);
        m.updateFrustumPlanes(this);
        return m;
    }

    /**
     * Calculate the frustum planes in world coordinates
     * using the passed premultiplied P*MV (column major order) matrix.
     * <p>
     * Frustum plane's normals will point to the inside of the viewing frustum,
     * as required by this class.
     * </p>
     * @see Matrix4f#updateFrustumPlanes(Frustum)
     */
    public Frustum updateFrustumPlanes(final Matrix4f pmv) {
        return pmv.updateFrustumPlanes(this);
    }

    /**
     * Calculate the frustum planes using the given {@link Cube}.
     * <p>
     * One useful application is to {@link Cube#transform(Matrix4f) transform}
     * an {@link AABBox}, see {@link Cube#Cube(AABBox)} from its object-space
     * into model-view (Mv) and produce the {@link Frustum} planes using this method
     * for CPU side object culling and GPU shader side fragment clipping.
     * </p>
     * <p>
     * Frustum plane's normals will point to the inside of the viewing frustum,
     * as required by this class.
     * </p>
     * @param c the {@link Cube} source
     * @return this frustum for chaining
     * @see Cube#updateFrustumPlanes(Frustum)
     * @see Cube#Cube(AABBox)
     * @see Cube#transform(Matrix4f)
     */
    public Frustum updateFrustumPlanes(final Cube c) {
        return c.updateFrustumPlanes(this);
    }

    private static final boolean intersects(final Plane p, final AABBox box) {
	    final Vec3f lo = box.getLow();
	    final Vec3f hi = box.getHigh();

		return p.distanceTo(lo.x(), lo.y(), lo.z()) > 0.0f ||
		       p.distanceTo(hi.x(), lo.y(), lo.z()) > 0.0f ||
		       p.distanceTo(lo.x(), hi.y(), lo.z()) > 0.0f ||
		       p.distanceTo(hi.x(), hi.y(), lo.z()) > 0.0f ||
		       p.distanceTo(lo.x(), lo.y(), hi.z()) > 0.0f ||
		       p.distanceTo(hi.x(), lo.y(), hi.z()) > 0.0f ||
		       p.distanceTo(lo.x(), hi.y(), hi.z()) > 0.0f ||
		       p.distanceTo(hi.x(), hi.y(), hi.z()) > 0.0f;
	}

	/**
	 * Returns whether the given {@link AABBox} is completely outside of this frustum.
	 * <p>
	 * Note: If method returns false, the box may only be partially inside, i.e. intersects with this frustum
	 * </p>
	 */
    public final boolean isOutside(final AABBox box) {
        return !intersects(planes[0], box) ||
               !intersects(planes[1], box) ||
               !intersects(planes[2], box) ||
               !intersects(planes[3], box) ||
               !intersects(planes[4], box) ||
               !intersects(planes[5], box);
    }

    private static final boolean intersects(final Plane p, final Cube c) {
        return p.distanceTo(c.lbf) > 0.0f ||
               p.distanceTo(c.rbf) > 0.0f ||
               p.distanceTo(c.rtf) > 0.0f ||
               p.distanceTo(c.ltf) > 0.0f ||
               p.distanceTo(c.lbn) > 0.0f ||
               p.distanceTo(c.rbn) > 0.0f ||
               p.distanceTo(c.rtn) > 0.0f ||
               p.distanceTo(c.ltn) > 0.0f;
    }

    /**
     * Returns whether the given {@link Cube} is completely outside of this frustum.
     * <p>
     * Note: If method returns false, the box may only be partially inside, i.e. intersects with this frustum
     * </p>
     */
    public final boolean isOutside(final Cube c) {
        return !intersects(planes[0], c) ||
               !intersects(planes[1], c) ||
               !intersects(planes[2], c) ||
               !intersects(planes[3], c) ||
               !intersects(planes[4], c) ||
               !intersects(planes[5], c);
    }


    public static enum Location { OUTSIDE, INSIDE, INTERSECT };

    /**
     * Classifies the given {@link Vec3f} point whether it is outside, inside or on a plane of this frustum.
     *
     * @param p the point
     * @return {@link Location} of point related to frustum planes
     */
    public final Location classifyPoint(final Vec3f p) {
        Location res = Location.INSIDE;

        for (int i = 0; i < 6; ++i) {
            final float d = planes[i].distanceTo(p);
            if ( d < 0.0f ) {
                return Location.OUTSIDE;
            } else if ( d == 0.0f ) {
                res = Location.INTERSECT;
            }
        }
        return res;
    }

    /**
     * Returns whether the given {@link Vec3f} point is completely outside of this frustum.
     *
     * @param p the point
     * @return true if outside of the frustum, otherwise inside or on a plane
     */
    public final boolean isOutside(final Vec3f p) {
        return planes[0].distanceTo(p) < 0.0f ||
               planes[1].distanceTo(p) < 0.0f ||
               planes[2].distanceTo(p) < 0.0f ||
               planes[3].distanceTo(p) < 0.0f ||
               planes[4].distanceTo(p) < 0.0f ||
               planes[5].distanceTo(p) < 0.0f;
    }

    /**
     * Classifies the given sphere whether it is is outside, intersecting or inside of this frustum.
     *
     * @param p center of the sphere
     * @param radius radius of the sphere
     * @return {@link Location} of point related to frustum planes
     */
    public final Location classifySphere(final Vec3f p, final float radius) {
        Location res = Location.INSIDE; // fully inside

        for (int i = 0; i < 6; ++i) {
            final float d = planes[i].distanceTo(p);
            if ( d < -radius ) {
                // fully outside
                return Location.OUTSIDE;
            } else if (d < radius ) {
                // intersecting
                res = Location.INTERSECT;
            }
        }
        return res;
    }

    /**
     * Returns whether the given sphere is completely outside of this frustum.
     *
     * @param p center of the sphere
     * @param radius radius of the sphere
     * @return true if outside of the frustum, otherwise inside or intersecting
     */
    public final boolean isSphereOutside(final Vec3f p, final float radius) {
        return Location.OUTSIDE == classifySphere(p, radius);
    }

    public StringBuilder toString(StringBuilder sb) {
        if( null == sb ) {
            sb = new StringBuilder();
        }
        sb.append("Frustum[Planes[").append(System.lineSeparator())
        .append(" L: ").append(planes[0]).append(", ").append(System.lineSeparator())
        .append(" R: ").append(planes[1]).append(", ").append(System.lineSeparator())
        .append(" B: ").append(planes[2]).append(", ").append(System.lineSeparator())
        .append(" T: ").append(planes[3]).append(", ").append(System.lineSeparator())
        .append(" N: ").append(planes[4]).append(", ").append(System.lineSeparator())
        .append(" F: ").append(planes[5]).append("], ").append(System.lineSeparator())
        .append("]");
        return sb;
    }

	@Override
	public String toString() {
	    return toString(null).toString();
	}
}
