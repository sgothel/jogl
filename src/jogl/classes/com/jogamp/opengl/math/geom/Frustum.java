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

import jogamp.common.os.PlatformPropsImpl;

import com.jogamp.common.os.Platform;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.FovHVHalves;

/**
 * Providing frustum {@link #getPlanes() planes} derived by different inputs
 * ({@link #updateByPMV(float[], int) P*MV}, ..) used to classify objects
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
	 * @see #updateByPMV(float[], int)
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
        public final float[] n = new float[3];

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
            return n[0] * x + n[1] * y + n[2] * z + d;
        }

        /** Return distance of plane to given point, see {@link #distanceTo(float, float, float)}. */
        public final float distanceTo(final float[] p) {
            return n[0] * p[0] + n[1] * p[1] + n[2] * p[2] + d;
        }

        @Override
        public String toString() {
            return "Plane[ [ " + n[0] + ", " + n[1] + ", " + n[2] + " ], " + d + "]";
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
     *       into the given float[16] as a perspective matrix (column major order) first,
     *       see {@link FloatUtil#makePerspective(float[], int, boolean, FovHVHalves, float, float)}.</li>
     *   <li>Then the float[16] perspective matrix is used to {@link #updateByPMV(float[], int)} this instance.</li>
     * </ul>
     * </p>
     * <p>
     * Frustum plane's normals will point to the inside of the viewing frustum,
     * as required by this class.
     * </p>
     *
     * @param m 4x4 matrix in column-major order (also result)
     * @param m_offset offset in given array <i>m</i>, i.e. start of the 4x4 matrix
     * @param initM if true, given matrix will be initialized w/ identity matrix,
     *              otherwise only the frustum fields are set.
     * @param fovDesc {@link Frustum} {@link FovDesc}
     * @return given matrix for chaining
     * @see FloatUtil#makePerspective(float[], int, boolean, FovHVHalves, float, float)
     */
    public float[] updateByFovDesc(final float[] m, final int m_offset, final boolean initM,
                                   final FovDesc fovDesc) {
        FloatUtil.makePerspective(m, m_offset, initM, fovDesc.fovhv, fovDesc.zNear, fovDesc.zFar);
        updateByPMV(m, 0);
        return m;
    }

    /**
     * Calculate the frustum planes in world coordinates
     * using the passed float[16] as premultiplied P*MV (column major order).
     * <p>
     * Frustum plane's normals will point to the inside of the viewing frustum,
     * as required by this class.
     * </p>
     */
    public void updateByPMV(final float[] pmv, final int pmv_off) {
        // Left:   a = m41 + m11, b = m42 + m12, c = m43 + m13, d = m44 + m14  - [1..4] column-major
        // Left:   a = m30 + m00, b = m31 + m01, c = m32 + m02, d = m33 + m03  - [0..3] column-major
        {
            final Plane p = planes[LEFT];
            final float[] p_n = p.n;
            p_n[0] = pmv[ pmv_off + 3 + 0 * 4 ] + pmv[ pmv_off + 0 + 0 * 4 ];
            p_n[1] = pmv[ pmv_off + 3 + 1 * 4 ] + pmv[ pmv_off + 0 + 1 * 4 ];
            p_n[2] = pmv[ pmv_off + 3 + 2 * 4 ] + pmv[ pmv_off + 0 + 2 * 4 ];
            p.d    = pmv[ pmv_off + 3 + 3 * 4 ] + pmv[ pmv_off + 0 + 3 * 4 ];
        }

        // Right:  a = m41 - m11, b = m42 - m12, c = m43 - m13, d = m44 - m14  - [1..4] column-major
        // Right:  a = m30 - m00, b = m31 - m01, c = m32 - m02, d = m33 - m03  - [0..3] column-major
        {
            final Plane p = planes[RIGHT];
            final float[] p_n = p.n;
            p_n[0] = pmv[ pmv_off + 3 + 0 * 4 ] - pmv[ pmv_off + 0 + 0 * 4 ];
            p_n[1] = pmv[ pmv_off + 3 + 1 * 4 ] - pmv[ pmv_off + 0 + 1 * 4 ];
            p_n[2] = pmv[ pmv_off + 3 + 2 * 4 ] - pmv[ pmv_off + 0 + 2 * 4 ];
            p.d    = pmv[ pmv_off + 3 + 3 * 4 ] - pmv[ pmv_off + 0 + 3 * 4 ];
        }

        // Bottom: a = m41 + m21, b = m42 + m22, c = m43 + m23, d = m44 + m24  - [1..4] column-major
        // Bottom: a = m30 + m10, b = m31 + m11, c = m32 + m12, d = m33 + m13  - [0..3] column-major
        {
            final Plane p = planes[BOTTOM];
            final float[] p_n = p.n;
            p_n[0] = pmv[ pmv_off + 3 + 0 * 4 ] + pmv[ pmv_off + 1 + 0 * 4 ];
            p_n[1] = pmv[ pmv_off + 3 + 1 * 4 ] + pmv[ pmv_off + 1 + 1 * 4 ];
            p_n[2] = pmv[ pmv_off + 3 + 2 * 4 ] + pmv[ pmv_off + 1 + 2 * 4 ];
            p.d    = pmv[ pmv_off + 3 + 3 * 4 ] + pmv[ pmv_off + 1 + 3 * 4 ];
        }

        // Top:   a = m41 - m21, b = m42 - m22, c = m43 - m23, d = m44 - m24  - [1..4] column-major
        // Top:   a = m30 - m10, b = m31 - m11, c = m32 - m12, d = m33 - m13  - [0..3] column-major
        {
            final Plane p = planes[TOP];
            final float[] p_n = p.n;
            p_n[0] = pmv[ pmv_off + 3 + 0 * 4 ] - pmv[ pmv_off + 1 + 0 * 4 ];
            p_n[1] = pmv[ pmv_off + 3 + 1 * 4 ] - pmv[ pmv_off + 1 + 1 * 4 ];
            p_n[2] = pmv[ pmv_off + 3 + 2 * 4 ] - pmv[ pmv_off + 1 + 2 * 4 ];
            p.d    = pmv[ pmv_off + 3 + 3 * 4 ] - pmv[ pmv_off + 1 + 3 * 4 ];
        }

        // Near:  a = m41 + m31, b = m42 + m32, c = m43 + m33, d = m44 + m34  - [1..4] column-major
        // Near:  a = m30 + m20, b = m31 + m21, c = m32 + m22, d = m33 + m23  - [0..3] column-major
        {
            final Plane p = planes[NEAR];
            final float[] p_n = p.n;
            p_n[0] = pmv[ pmv_off + 3 + 0 * 4 ] + pmv[ pmv_off + 2 + 0 * 4 ];
            p_n[1] = pmv[ pmv_off + 3 + 1 * 4 ] + pmv[ pmv_off + 2 + 1 * 4 ];
            p_n[2] = pmv[ pmv_off + 3 + 2 * 4 ] + pmv[ pmv_off + 2 + 2 * 4 ];
            p.d    = pmv[ pmv_off + 3 + 3 * 4 ] + pmv[ pmv_off + 2 + 3 * 4 ];
        }

        // Far:   a = m41 - m31, b = m42 - m32, c = m43 - m33, d = m44 - m34  - [1..4] column-major
        // Far:   a = m30 - m20, b = m31 - m21, c = m32 + m22, d = m33 + m23  - [0..3] column-major
        {
            final Plane p = planes[FAR];
            final float[] p_n = p.n;
            p_n[0] = pmv[ pmv_off + 3 + 0 * 4 ] - pmv[ pmv_off + 2 + 0 * 4 ];
            p_n[1] = pmv[ pmv_off + 3 + 1 * 4 ] - pmv[ pmv_off + 2 + 1 * 4 ];
            p_n[2] = pmv[ pmv_off + 3 + 2 * 4 ] - pmv[ pmv_off + 2 + 2 * 4 ];
            p.d    = pmv[ pmv_off + 3 + 3 * 4 ] - pmv[ pmv_off + 2 + 3 * 4 ];
        }

        // Normalize all planes
        for (int i = 0; i < 6; ++i) {
            final Plane p = planes[i];
            final float[] p_n = p.n;
            final double invl = Math.sqrt(p_n[0] * p_n[0] + p_n[1] * p_n[1] + p_n[2] * p_n[2]);

            p_n[0] /= invl;
            p_n[1] /= invl;
            p_n[2] /= invl;
            p.d /= invl;
        }
    }

	private static final boolean isOutsideImpl(final Plane p, final AABBox box) {
	    final float[] low = box.getLow();
	    final float[] high = box.getHigh();

		if ( p.distanceTo(low[0],  low[1],  low[2])  > 0.0f ||
		     p.distanceTo(high[0], low[1],  low[2])  > 0.0f ||
		     p.distanceTo(low[0],  high[1], low[2])  > 0.0f ||
		     p.distanceTo(high[0], high[1], low[2])  > 0.0f ||
		     p.distanceTo(low[0],  low[1],  high[2]) > 0.0f ||
		     p.distanceTo(high[0], low[1],  high[2]) > 0.0f ||
		     p.distanceTo(low[0],  high[1], high[2]) > 0.0f ||
		     p.distanceTo(high[0], high[1], high[2]) > 0.0f ) {
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
    public final Location classifyPoint(final float[] p) {
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
    public final boolean isPointOutside(final float[] p) {
        return Location.OUTSIDE == classifyPoint(p);
    }

    /**
     * Check to see if a sphere is outside, intersecting or inside of the frustum.
     *
     * @param p center of the sphere
     * @param radius radius of the sphere
     * @return {@link Location} of point related to frustum planes
     */
    public final Location classifySphere(final float[] p, final float radius) {
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
    public final boolean isSphereOutside(final float[] p, final float radius) {
        return Location.OUTSIDE == classifySphere(p, radius);
    }

    public StringBuilder toString(StringBuilder sb) {
        if( null == sb ) {
            sb = new StringBuilder();
        }
        sb.append("Frustum[ Planes[ ").append(PlatformPropsImpl.NEWLINE)
        .append(" L: ").append(planes[0]).append(", ").append(PlatformPropsImpl.NEWLINE)
        .append(" R: ").append(planes[1]).append(", ").append(PlatformPropsImpl.NEWLINE)
        .append(" B: ").append(planes[2]).append(", ").append(PlatformPropsImpl.NEWLINE)
        .append(" T: ").append(planes[3]).append(", ").append(PlatformPropsImpl.NEWLINE)
        .append(" N: ").append(planes[4]).append(", ").append(PlatformPropsImpl.NEWLINE)
        .append(" F: ").append(planes[5]).append("], ").append(PlatformPropsImpl.NEWLINE)
        .append("]");
        return sb;
    }

	@Override
	public String toString() {
	    return toString(null).toString();
	}
}
