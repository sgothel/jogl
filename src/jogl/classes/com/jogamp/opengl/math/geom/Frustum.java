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

/**
 * Derived Frustum of premultiplied projection * modelview matrix
 * exposing {@link #isOutside(AABBox)} test and the {@link #getPlanes() planes} itself.
 */
public class Frustum {
	protected Plane[] planes = new Plane[6];
	
	/**
	 * Creates an undefined instance w/o calculating the frustum.
	 */
    public Frustum() {
        for (int i = 0; i < 6; ++i) {
            planes[i] = new Plane();
        }
    }
    
    /**
     * Creates a defined instance w/ calculating the frustum
     * using the passed float[16] as premultiplied MV*P (column major order)
     */
	public Frustum(float[] mvp, int pmv_off) {
		for (int i = 0; i < 6; ++i) {
			planes[i] = new Plane();
		}
		update(mvp, pmv_off);
	}

    public static class Plane {
        /** Normal of the plane */
        public final float[] n = new float[3];
        
        /** Distance to origin */
        public float d;

        /** Return distance of plane to given point */
        public final float distanceTo(float x, float y, float z) {
            return (n[0] * x) + (n[1] * y) + (n[2] * z) + d;
        }

        /** Return distance of plane to given point */
        public final float distanceTo(float[] p) {
            return (n[0] * p[0]) + (n[1] * p[1]) + (n[2] * p[2]) + d;
        }
        
        @Override
        public String toString() {
            return "Plane[ " + n + ", " + d + "]";
        }
    }

    public final Plane[] getPlanes() { return planes; }
    
    /**
     * Re-calculate the frustum
     * using the passed float[16] as premultiplied MV*P (column major order).
     */
    public void update(float[] mvp, int mvp_off) {
        // Left: [30+00, 31+01, 32+02, 33+03]
        // comboMatrix.m[12] + comboMatrix.m[0];
        {
            final Plane p = planes[0];
            final float[] p_n = p.n;
            p_n[0] = mvp[ mvp_off + 12 ] + mvp[ mvp_off + 0 ];
            p_n[1] = mvp[ mvp_off + 13 ] + mvp[ mvp_off + 1 ];
            p_n[2] = mvp[ mvp_off + 14 ] + mvp[ mvp_off + 2 ];
            p.d = mvp[ mvp_off + 15 ] + mvp[ mvp_off + 3 ];
        }

        // Right: [30-00, 31-01, 32-02, 33-03]
        {
            final Plane p = planes[1];
            final float[] p_n = p.n;
            p_n[0] = mvp[ mvp_off + 12 ] - mvp[ mvp_off + 0 ];
            p_n[1] = mvp[ mvp_off + 13 ] - mvp[ mvp_off + 1 ];
            p_n[2] = mvp[ mvp_off + 14 ] - mvp[ mvp_off + 2 ];
            p.d = mvp[ mvp_off + 15 ] - mvp[ mvp_off + 3 ];
        }

        // Bottom: [30+10, 31+11, 32+12, 33+13]
        {
            final Plane p = planes[2];
            final float[] p_n = p.n;
            p_n[0] = mvp[ mvp_off + 12 ] + mvp[ mvp_off + 4 ];
            p_n[1] = mvp[ mvp_off + 13 ] + mvp[ mvp_off + 5 ];
            p_n[2] = mvp[ mvp_off + 14 ] + mvp[ mvp_off + 6 ];
            p.d = mvp[ mvp_off + 15 ] + mvp[ mvp_off + 7 ];
        }

        // Top: [30-10, 31-11, 32-12, 33-13]
        {
            final Plane p = planes[3];
            final float[] p_n = p.n;
            p_n[0] = mvp[ mvp_off + 12 ] - mvp[ mvp_off + 4 ];
            p_n[1] = mvp[ mvp_off + 13 ] - mvp[ mvp_off + 5 ];
            p_n[2] = mvp[ mvp_off + 14 ] - mvp[ mvp_off + 6 ];
            p.d = mvp[ mvp_off + 15 ] - mvp[ mvp_off + 7 ];
        }

        // Near: [30+20, 31+21, 32+22, 33+23]
        {
            final Plane p = planes[4];
            final float[] p_n = p.n;
            p_n[0] = mvp[ mvp_off + 12 ] + mvp[ mvp_off + 8 ];
            p_n[1] = mvp[ mvp_off + 13 ] + mvp[ mvp_off + 9 ];
            p_n[2] = mvp[ mvp_off + 14 ] + mvp[ mvp_off + 10 ];
            p.d = mvp[ mvp_off + 15 ] + mvp[ mvp_off + 11 ];
        }

        // Far: [30-20, 31-21, 32-22, 33-23]
        {
            final Plane p = planes[5];
            final float[] p_n = p.n;
            p_n[0] = mvp[ mvp_off + 12 ] - mvp[ mvp_off + 8 ];
            p_n[1] = mvp[ mvp_off + 13 ] - mvp[ mvp_off + 9 ];
            p_n[2] = mvp[ mvp_off + 14 ] - mvp[ mvp_off + 10 ];
            p.d = mvp[ mvp_off + 15 ] - mvp[ mvp_off + 11 ];
        }

        for (int i = 0; i < 6; ++i) {
            final Plane p = planes[i];
            final float[] p_n = p.n;
            final double invl = Math.sqrt(p_n[0] * p_n[0] + p_n[1] * p_n[1] + p_n[2] * p_n[2]);

            p_n[0] *= invl;
            p_n[1] *= invl;
            p_n[2] *= invl;
            p.d *= invl;
        }
    }

	private static final boolean quickClassify(Plane p, AABBox box) {
	    final float[] low = box.getLow();
	    final float[] high = box.getHigh();
	    
		if (p.distanceTo(low[0],  low[1],  low[2]) > 0.0f)
			return true;
		if (p.distanceTo(high[0], low[1],  low[2]) > 0.0f)
			return true;
		if (p.distanceTo(low[0],  high[1], low[2]) > 0.0f)
			return true;
		if (p.distanceTo(high[0], high[1], low[2]) > 0.0f)
			return true;
		if (p.distanceTo(low[0],  low[1],  high[2]) > 0.0f)
			return true;
		if (p.distanceTo(high[0], low[1],  high[2]) > 0.0f)
			return true;
		if (p.distanceTo(low[0],  high[1], high[2]) > 0.0f)
			return true;
		if (p.distanceTo(high[0], high[1], high[2]) > 0.0f)
			return true;

		return false;
	}

	/**
	 * Quick check to see if an orthogonal bounding box is completly outside of the frustum.
	 * <p>
	 * Note: If method returns false, the box may be only partially inside.
	 * </p>
	 */
    public final boolean isOutside(AABBox box) {
        for (int i = 0; i < 6; ++i) {
            if (!quickClassify(planes[i], box)) {
                // fully outside
                return true;
            }
        }
        // We make no attempt to determine whether it's fully inside or not.
        return false;
    }
    
    public StringBuilder toString(StringBuilder sb) {
        if( null == sb ) {
            sb = new StringBuilder();
        }
        sb.append("Frustum[ Planes[ ").append(planes[0]).append(", ");
        sb.append(planes[1]).append(", ").append(planes[2]).append(", ").append(planes[3]).append(", ").append(planes[4]).append(", ");
        sb.append(planes[5]).append(" ] ]");
        return sb;
    }
    
	@Override
	public String toString() {
	    return toString(null).toString();
	}
}
