package com.jogamp.opengl.util;

import java.nio.FloatBuffer;

// import org.openmali.spatial.bodies.Box;
// import org.openmali.spatial.bodies.Classifier;
// import org.openmali.vecmath2.Matrix4f;

public class Frustum2 {
	protected Plane[] planes = new Plane[6];
	protected PMVMatrix pmvMatrix;
	protected FloatBuffer pmv = FloatBuffer.allocate(16);
	protected int pmvOffset;

	public Frustum2(PMVMatrix matrix) {
		setMatrix(matrix);
		for (int i = 0; i < 6; ++i) {
			planes[i] = new Plane();
		}
	}

	public void setMatrix(PMVMatrix matrix) {
		this.pmvMatrix = matrix;
		// pmv = pmvMatrix.glGetPMvMatrixf();
		// pmvOffset = pmv.position();
		makePmvMatrix();
	}

	protected Matrix4f proj;
	protected Matrix4f modl;

	private FloatBuffer b;
	private int bOffset;

	private float f(int offset) {
		return b.get(bOffset + offset);
	}

	private Matrix4f getMatrix4f(FloatBuffer b) {
		this.b = b;
		bOffset = b.position();
		return new Matrix4f(f(0), f(1), f(2), f(3), f(4), f(5), f(6)/* 12 */, f(7), f(8), f(9), f(10), f(11), f(12),
				f(13), f(14), f(15));
	}

	public void makePmvMatrix() {
		getMatrix4f(pmvMatrix.glGetMvMatrixf()).mul(getMatrix4f(pmvMatrix.glGetPMatrixf())).writeToBuffer(pmv, true,
				false);
	}

	protected class Vector3f {
		public float x;
		public float y;
		public float z;

		@Override
		public String toString() {
			return "{" + x + "," + y + "," + z + "}";
		}
	}

	protected class Plane {
		public Vector3f n = new Vector3f();
		public float d;

		public final float distanceTo(float x, float y, float z) {
			return (n.x * x) + (n.y * y) + (n.z * z) + d;
		}

		@Override
		public String toString() {
			return "Plane[" + n + ", " + d + "]";
		}
	}

	protected float[] getMatrixFloat(FloatBuffer b) {
		if (pmvMatrix.usesBackingArray()) {
			return b.array();
		} else {
			int p = b.position();
			float[] pm = new float[16];
			b.get(pm, p, 16);
			b.position(p);
			return pm;
		}
	}

	protected float m(int a) {
		return pmv.get(a);
	}

	private static final boolean quickClassify(Plane p, Box box) {
		if (p.distanceTo(box.getLowerX(), box.getLowerY(), box.getLowerZ()) > 0.0f)
			return (true);
		if (p.distanceTo(box.getUpperX(), box.getLowerY(), box.getLowerZ()) > 0.0f)
			return (true);
		if (p.distanceTo(box.getLowerX(), box.getUpperY(), box.getLowerZ()) > 0.0f)
			return (true);
		if (p.distanceTo(box.getUpperX(), box.getUpperY(), box.getLowerZ()) > 0.0f)
			return (true);
		if (p.distanceTo(box.getLowerX(), box.getLowerY(), box.getUpperZ()) > 0.0f)
			return (true);
		if (p.distanceTo(box.getUpperX(), box.getLowerY(), box.getUpperZ()) > 0.0f)
			return (true);
		if (p.distanceTo(box.getLowerX(), box.getUpperY(), box.getUpperZ()) > 0.0f)
			return (true);
		if (p.distanceTo(box.getUpperX(), box.getUpperY(), box.getUpperZ()) > 0.0f)
			return (true);

		return (false);
	}

	/**
	 * Quick check to see if an orthogonal bounding box is inside the frustum
	 */
	public final Classifier.Classification quickClassify(Box box) {
		for (int i = 0; i < 6; ++i) {
			if (!quickClassify(planes[i], box))
				return (Classifier.Classification.OUTSIDE);
		}

		// We make no attempt to determine whether it's fully inside or not.
		return (Classifier.Classification.SPANNING);
	}

	protected float[] mat = new float[16];

	public void compute() {
		// Left: [30+00, 31+01, 32+02, 33+03]
		// comboMatrix.m[12] + comboMatrix.m[0];

		planes[0].n.x = m(12) + m(0);
		planes[0].n.y = m(13) + m(1);
		planes[0].n.z = m(14) + m(2);
		planes[0].d = m(15) + m(3);

		// Right: [30-00, 31-01, 32-02, 33-03]

		planes[1].n.x = m(12) - m(0);
		planes[1].n.y = m(13) - m(1);
		planes[1].n.z = m(14) - m(2);
		planes[1].d = m(15) - m(3);

		// Bottom: [30+10, 31+11, 32+12, 33+13]

		planes[2].n.x = m(12) + m(4);
		planes[2].n.y = m(13) + m(5);
		planes[2].n.z = m(14) + m(6);
		planes[2].d = m(15) + m(7);

		// Top: [30-10, 31-11, 32-12, 33-13]

		planes[3].n.x = m(12) - m(4);
		planes[3].n.y = m(13) - m(5);
		planes[3].n.z = m(14) - m(6);
		planes[3].d = m(15) - m(7);

		// Far: [30-20, 31-21, 32-22, 33-23]

		planes[5].n.x = m(12) - m(8);
		planes[5].n.y = m(13) - m(9);
		planes[5].n.z = m(14) - m(10);
		planes[5].d = m(15) - m(11);

		// Near: [30+20, 31+21, 32+22, 33+23]

		planes[4].n.x = m(12) + m(8);
		planes[4].n.y = m(13) + m(9);
		planes[4].n.z = m(14) + m(10);
		planes[4].d = m(15) + m(11);


			for (int i = 0; i < 6; ++i) {
				double invl = Math.sqrt(planes[i].n.x * planes[i].n.x + planes[i].n.y * planes[i].n.y + planes[i].n.z
						* planes[i].n.z);

				planes[i].n.x *= invl;
				planes[i].n.y *= invl;
				planes[i].n.z *= invl;
				planes[i].d *= invl;
			}
	}

	@Override
	public String toString() {
		return "m[" + m(0) + "|" + m(1) + "|" + m(2) + "|" + m(3) + "|" + m(4) + "|" + m(5) + "|" + m(6) + "|" + m(7)
				+ "|" + m(8) + "|" + m(9) + "|" + m(10) + "|" + m(11) + "|" + m(12) + "|" + m(13) + "|" + m(14) + "|"
				+ m(15) + "]" + "Frustum2[" + planes[0] + ", " + planes[1] + ", " + planes[2] + ", " + planes[3] + ", "
				+ planes[4] + ", " + planes[5] + "]";
	}
}
