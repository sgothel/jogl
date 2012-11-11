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

import com.jogamp.common.os.Platform;
import com.jogamp.opengl.math.FloatUtil;

public class Frustum {
	protected Plane[] planes = new Plane[6];
	protected float[] pmv;

	public Frustum(float[] pmv) {
	    this.pmv = pmv;
		for (int i = 0; i < 6; ++i) {
			planes[i] = new Plane();
		}
	}

	public void setMatrix(float[] pmv) {
        this.pmv = pmv;
	}

    public final Plane[] getPlanes() { return planes; }
    
    public final float[] getPMV() { return pmv; }
    
	public class Plane {
		public final float[] n = new float[3];
		public float d;

		public final float distanceTo(float x, float y, float z) {
			return (n[0] * x) + (n[1] * y) + (n[2] * z) + d;
		}

		@Override
		public String toString() {
			return "Plane[ [ " + n[0] + ", " + n[1] + ", " + n[2] + " ], " + d + "]";
		}
	}

	private static final boolean quickClassify(Plane p, AABBox box) {
	    final float[] low = box.getLow();
	    final float[] high = box.getHigh();
	    
		if (p.distanceTo(low[0],  low[1],  low[2]) > 0.0f)
			return (true);
		if (p.distanceTo(high[0], low[1],  low[2]) > 0.0f)
			return (true);
		if (p.distanceTo(low[0],  high[1], low[2]) > 0.0f)
			return (true);
		if (p.distanceTo(high[0], high[1], low[2]) > 0.0f)
			return (true);
		if (p.distanceTo(low[0],  low[1],  high[2]) > 0.0f)
			return (true);
		if (p.distanceTo(high[0], low[1],  high[2]) > 0.0f)
			return (true);
		if (p.distanceTo(low[0],  high[1], high[2]) > 0.0f)
			return (true);
		if (p.distanceTo(high[0], high[1], high[2]) > 0.0f)
			return (true);

		return (false);
	}

	/**
	 * Quick check to see if an orthogonal bounding box is inside the frustum
	 */
	public final boolean isInside(AABBox box) {
		for (int i = 0; i < 6; ++i) {
			if (!quickClassify(planes[i], box))
				return false;
		}
		// We make no attempt to determine whether it's fully inside or not.
		return true;
	}

	public void compute() {
		// Left: [30+00, 31+01, 32+02, 33+03]
		// comboMatrix.m[12] + comboMatrix.m[0];

		planes[0].n[0] = pmv[12] + pmv[0];
		planes[0].n[1] = pmv[13] + pmv[1];
		planes[0].n[2] = pmv[14] + pmv[2];
		planes[0].d = pmv[15] + pmv[3];

		// Right: [30-00, 31-01, 32-02, 33-03]

		planes[1].n[0] = pmv[12] - pmv[0];
		planes[1].n[1] = pmv[13] - pmv[1];
		planes[1].n[2] = pmv[14] - pmv[2];
		planes[1].d = pmv[15] - pmv[3];

		// Bottom: [30+10, 31+11, 32+12, 33+13]

		planes[2].n[0] = pmv[12] + pmv[4];
		planes[2].n[1] = pmv[13] + pmv[5];
		planes[2].n[2] = pmv[14] + pmv[6];
		planes[2].d = pmv[15] + pmv[7];

		// Top: [30-10, 31-11, 32-12, 33-13]

		planes[3].n[0] = pmv[12] - pmv[4];
		planes[3].n[1] = pmv[13] - pmv[5];
		planes[3].n[2] = pmv[14] - pmv[6];
		planes[3].d = pmv[15] - pmv[7];

		// Far: [30-20, 31-21, 32-22, 33-23]

		planes[5].n[0] = pmv[12] - pmv[8];
		planes[5].n[1] = pmv[13] - pmv[9];
		planes[5].n[2] = pmv[14] - pmv[10];
		planes[5].d = pmv[15] - pmv[11];

		// Near: [30+20, 31+21, 32+22, 33+23]

		planes[4].n[0] = pmv[12] + pmv[8];
		planes[4].n[1] = pmv[13] + pmv[9];
		planes[4].n[2] = pmv[14] + pmv[10];
		planes[4].d = pmv[15] + pmv[11];


		for (int i = 0; i < 6; ++i) {
		    final float[] p_n = planes[i].n;
			final double invl = Math.sqrt(p_n[0] * p_n[0] + p_n[1] * p_n[1] + p_n[2] * p_n[2]);

			p_n[0] *= invl;
			p_n[1] *= invl;
			p_n[2] *= invl;
			planes[i].d *= invl;
		}
	}

    public StringBuilder toString(StringBuilder sb) {
        if( null == sb ) {
            sb = new StringBuilder();
        }
        sb.append("Frustum[ P*MV[").append(Platform.NEWLINE);
        FloatUtil.matrixToString(sb, "p*mv", null, pmv, 0, 4, 4, false);
        sb.append("], Planes[").append(planes[0]).append(", ");
        sb.append(planes[1]).append(", ").append(planes[2]).append(", ").append(planes[3]).append(", ").append(planes[4]).append(", ");
        sb.append(planes[5]).append("] ]");
        return sb;
    }
    
	@Override
	public String toString() {
	    return toString(null).toString();
	}
}
