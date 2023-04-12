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

import com.jogamp.opengl.math.FovHVHalves;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Vec3f;

/**
 * Providing frustum {@link #getPlanes() planes} derived by different inputs
 * ({@link #updateFrustumPlanes(float[], int) P*MV}, ..) used to classify objects
 * <ul>
 *   <li> {@link #classifyPoint(float[]) point} </li>
 *   <li> {@link #classifySphere(float[], float) sphere} </li>
 * </ul>
 * and to test whether they are outside
 * <ul>
 *   <li> {@link #isPointOutside(float[]) point} </li>
 *   <li> {@link #isSphereOutside(float[], float) sphere} </li>
 *   <li> {@link #isAABBoxOutside(AABBox) bounding-box} </li>
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
        for (int i = 0; i < 6; ++i) {
            planes[i] = new Plane();
        }
    }

	/**
	 * Plane equation := dot(n, x - p) = 0 ->  ax + bc + cx + d == 0
	 * <p>
	 * In order to work w/ {@link Frustum#isOutside(AABBox) isOutside(..)} methods,
	 * the normals have to point to the inside of the frustum.
	 * </p>
	 */
    public static class Plane {
        /** Normal of the plane */
        public final Vec3f n = new Vec3f();

        /** Distance to origin */
        public float d;

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
            return n.x() * p.x() + n.y() * p.y() + n.z() * p.z() + d;
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
     */
    public void updateFrustumPlanes(final Matrix4f pmv) {
        pmv.updateFrustumPlanes(this);
    }

	private static final boolean isOutsideImpl(final Plane p, final AABBox box) {
	    final Vec3f lo = box.getLow();
	    final Vec3f hi = box.getHigh();

		if ( p.distanceTo(lo.x(), lo.y(), lo.z()) > 0.0f ||
		     p.distanceTo(hi.x(), lo.y(), lo.z()) > 0.0f ||
		     p.distanceTo(lo.x(), hi.y(), lo.z()) > 0.0f ||
		     p.distanceTo(hi.x(), hi.y(), lo.z()) > 0.0f ||
		     p.distanceTo(lo.x(), lo.y(), hi.z()) > 0.0f ||
		     p.distanceTo(hi.x(), lo.y(), hi.z()) > 0.0f ||
		     p.distanceTo(lo.x(), hi.y(), hi.z()) > 0.0f ||
		     p.distanceTo(hi.x(), hi.y(), hi.z()) > 0.0f ) {
			return false;
		}
		return true;
	}

	/**
	 * Check to see if an axis aligned bounding box is completely outside of the frustum.
	 * <p>
	 * Note: If method returns false, the box may only be partially inside.
	 * </p>
	 */
    public final boolean isAABBoxOutside(final AABBox box) {
        for (int i = 0; i < 6; ++i) {
            if ( isOutsideImpl(planes[i], box) ) {
                // fully outside
                return true;
            }
        }
        // We make no attempt to determine whether it's fully inside or not.
        return false;
    }


    public static enum Location { OUTSIDE, INSIDE, INTERSECT };

    /**
     * Check to see if a point is outside, inside or on a plane of the frustum.
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
     * Check to see if a point is outside of the frustum.
     *
     * @param p the point
     * @return true if outside of the frustum, otherwise inside or on a plane
     */
    public final boolean isPointOutside(final Vec3f p) {
        return Location.OUTSIDE == classifyPoint(p);
    }

    /**
     * Check to see if a sphere is outside, intersecting or inside of the frustum.
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
     * Check to see if a sphere is outside of the frustum.
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
