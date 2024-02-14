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
package com.jogamp.math;

import java.util.List;

import com.jogamp.graph.geom.Vertex;
import com.jogamp.math.geom.plane.Winding;

public final class VectorUtil {
    /**
     * Return true if 2D vector components are zero, no {@link FloatUtil#EPSILON} is taken into consideration.
     */
    public static boolean isVec2Zero(final Vec3f vec) {
        return 0f == vec.x() && 0f == vec.y();
    }

    /**
     * Return true if all three vector components are zero, i.e. it's their absolute value < <code>epsilon</code>.
     * <p>
     * Implementation uses {@link FloatUtil#isZero(float, float)}, see API doc for details.
     * </p>
     */
    public static boolean isZero(final float x, final float y, final float z, final float epsilon) {
        return FloatUtil.isZero(x, epsilon) &&
               FloatUtil.isZero(y, epsilon) &&
               FloatUtil.isZero(z, epsilon) ;
    }

    /**
     * Return true if all three vector components are zero, i.e. it's their absolute value < {@link FloatUtil#EPSILON}.
     * <p>
     * Implementation uses {@link FloatUtil#isZero(float)}, see API doc for details.
     * </p>
     */
    public static boolean isZero(final float x, final float y, final float z) {
        return FloatUtil.isZero(x) &&
               FloatUtil.isZero(y) &&
               FloatUtil.isZero(z) ;
    }

    /**
     * Return the squared distance between the given two points described vector v1 and v2.
     * <p>
     * When comparing the relative distance between two points it is usually sufficient to compare the squared
     * distances, thus avoiding an expensive square root operation.
     * </p>
     */
    public static float distSquareVec3(final float[] v1, final float[] v2) {
        final float dx = v1[0] - v2[0];
        final float dy = v1[1] - v2[1];
        final float dz = v1[2] - v2[2];
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Return the distance between the given two points described vector v1 and v2.
     */
    public static float distVec3(final float[] v1, final float[] v2) {
        return FloatUtil.sqrt(distSquareVec3(v1, v2));
    }

    /**
     * Return the squared length of a vector, a.k.a the squared <i>norm</i> or squared <i>magnitude</i>
     */
    public static float normSquareVec2(final float[] vec) {
        return vec[0]*vec[0] + vec[1]*vec[1];
    }

    /**
     * Return the squared length of a vector, a.k.a the squared <i>norm</i> or squared <i>magnitude</i>
     */
    public static float normSquareVec3(final float[] vec) {
        return vec[0]*vec[0] + vec[1]*vec[1] + vec[2]*vec[2];
    }

    /**
     * Return the squared length of a vector, a.k.a the squared <i>norm</i> or squared <i>magnitude</i>
     */
    public static float normSquareVec3(final float[] vec, final int offset) {
        float v = vec[0+offset];
        float r = v*v;
        v = vec[1+offset];
        r += v*v;
        v = vec[2+offset];
        return r + v*v;
    }

    /**
     * Return the length of a vector, a.k.a the <i>norm</i> or <i>magnitude</i>
     */
    public static float normVec2(final float[] vec) {
        return FloatUtil.sqrt(normSquareVec2(vec));
    }

    /**
     * Normalize a vector in place
     * @param vector input vector
     * @return normalized output vector
     */
    public static float[] normalizeVec3(final float[] vector) {
        final float lengthSq = normSquareVec3(vector);
        if ( FloatUtil.isZero(lengthSq) ) {
            vector[0] = 0f;
            vector[1] = 0f;
            vector[2] = 0f;
        } else {
            final float invSqr = 1f / FloatUtil.sqrt(lengthSq);
            vector[0] *= invSqr;
            vector[1] *= invSqr;
            vector[2] *= invSqr;
        }
        return vector;
    }

    /**
     * Normalize a vector in place
     * @param vector input vector
     * @return normalized output vector
     */
    public static float[] normalizeVec3(final float[] vector, final int offset) {
        final float lengthSq = normSquareVec3(vector, offset);
        if ( FloatUtil.isZero(lengthSq) ) {
            vector[0+offset] = 0f;
            vector[1+offset] = 0f;
            vector[2+offset] = 0f;
        } else {
            final float invSqr = 1f / FloatUtil.sqrt(lengthSq);
            vector[0+offset] *= invSqr;
            vector[1+offset] *= invSqr;
            vector[2+offset] *= invSqr;
        }
        return vector;
    }

    /**
     * Scales a vector by param using given result float[], result = vector * scale
     * @param result vector for the result, may be vector (in-place)
     * @param vector input vector
     * @param scale single scale constant for all vector components
     * @return result vector for chaining
     */
    public static float[] scaleVec2(final float[] result, final float[] vector, final float scale) {
        result[0] = vector[0] * scale;
        result[1] = vector[1] * scale;
        return result;
    }

    /**
     * Scales a vector by param using given result float[], result = vector * scale
     * @param result vector for the result, may be vector (in-place)
     * @param vector input vector
     * @param scale 2 component scale constant for each vector component
     * @return result vector for chaining
     */
    public static float[] scaleVec2(final float[] result, final float[] vector, final float[] scale)
    {
        result[0] = vector[0] * scale[0];
        result[1] = vector[1] * scale[1];
        return result;
    }

    /**
     * Divides a vector by param using given result float[], result = vector / scale
     * @param result vector for the result, may be vector (in-place)
     * @param vector input vector
     * @param scale single scale constant for all vector components
     * @return result vector for chaining
     */
    public static float[] divVec2(final float[] result, final float[] vector, final float scale) {
        result[0] = vector[0] / scale;
        result[1] = vector[1] / scale;
        return result;
    }

    /**
     * Divides a vector by param using given result float[], result = vector / scale
     * @param result vector for the result, may be vector (in-place)
     * @param vector input vector
     * @param scale 2 component scale constant for each vector component
     * @return result vector for chaining
     */
    public static float[] divVec2(final float[] result, final float[] vector, final float[] scale)
    {
        result[0] = vector[0] / scale[0];
        result[1] = vector[1] / scale[1];
        return result;
    }

    /**
     * Adds two vectors, result = v1 + v2
     * @param result float[2] result vector, may be either v1 or v2 (in-place)
     * @param v1 vector 1
     * @param v2 vector 2
     * @return result vector for chaining
     */
    public static float[] addVec2(final float[] result, final float[] v1, final float[] v2) {
        result[0] = v1[0] + v2[0];
        result[1] = v1[1] + v2[1];
        return result;
    }

    /**
     * Subtracts two vectors, result = v1 - v2
     * @param result float[2] result vector, may be either v1 or v2 (in-place)
     * @param v1 vector 1
     * @param v2 vector 2
     * @return result vector for chaining
     */
    public static float[] subVec2(final float[] result, final float[] v1, final float[] v2) {
        result[0] = v1[0] - v2[0];
        result[1] = v1[1] - v2[1];
        return result;
    }

    /**
     * cross product vec1 x vec2
     * @param v1 vector 1
     * @param v2 vector 2
     * @return the resulting vector
     */
    public static float[] crossVec3(final float[] r, final int r_offset, final float[] v1, final int v1_offset, final float[] v2, final int v2_offset)
    {
        r[0+r_offset] = v1[1+v1_offset] * v2[2+v2_offset] - v1[2+v1_offset] * v2[1+v2_offset];
        r[1+r_offset] = v1[2+v1_offset] * v2[0+v2_offset] - v1[0+v1_offset] * v2[2+v2_offset];
        r[2+r_offset] = v1[0+v1_offset] * v2[1+v2_offset] - v1[1+v1_offset] * v2[0+v2_offset];
        return r;
    }

    /**
     * Calculate the midpoint of two points
     * @param p1 first point vector
     * @param p2 second point vector
     * @return midpoint
     */
    public static Vec3f midpoint(final Vec3f result, final Vec3f p1, final Vec3f p2) {
        result.set( (p1.x() + p2.x())*0.5f,
                    (p1.y() + p2.y())*0.5f,
                    (p1.z() + p2.z())*0.5f );
        return result;
    }

    /**
     * Return the determinant of 3 vectors
     * @param a vector 1
     * @param b vector 2
     * @param c vector 3
     * @return the determinant value
     */
    public static float determinant(final Vec3f a, final Vec3f b, final Vec3f c) {
        return a.x()*b.y()*c.z() + a.y()*b.z()*c.x() + a.z()*b.x()*c.y() - a.x()*b.z()*c.y() - a.y()*b.x()*c.z() - a.z()*b.y()*c.x();
    }

    /**
     * Check if three vertices are collinear
     * @param v1 vertex 1
     * @param v2 vertex 2
     * @param v3 vertex 3
     * @return true if collinear, false otherwise
     */
    public static boolean isCollinear(final Vec3f v1, final Vec3f v2, final Vec3f v3) {
        return FloatUtil.isZero( determinant(v1, v2, v3) );
    }

    /**
     * Check if vertices in triangle circumcircle given {@code d} vertex, from paper by Guibas and Stolfi (1985).
     * <p>
     * Implementation uses double precision.
     * </p>
     * @param a triangle vertex 1
     * @param b triangle vertex 2
     * @param c triangle vertex 3
     * @param d vertex in question
     * @return true if the vertex d is inside the circle defined by the vertices a, b, c.
     */
    public static boolean isInCircle(final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c, final Vert2fImmutable d) {
        return inCircleVal(a, b, c, d) > DoubleUtil.EPSILON;
    }
    public static double inCircleVal(final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c, final Vert2fImmutable d) {
        // Operation costs:
        // - 4x (triAreaVec2: 5+, 2*) -> 20+, 8*
        // - plus 7+, 12*             -> 27+, 20*
        return sqlend(a.x(), a.y()) * triArea(b, c, d) -
               sqlend(b.x(), b.y()) * triArea(a, c, d) +
               sqlend(c.x(), c.y()) * triArea(a, b, d) -
               sqlend(d.x(), d.y()) * triArea(a, b, c);
    }
    private static double sqlend(final double x, final double y) {
        return x*x + y*y;
    }

    /**
     * Computes oriented double area of a triangle,
     * i.e. the 2x2 determinant with b-a and c-a per column.
     * <pre>
     *       | bx-ax, cx-ax |
     * det = | by-ay, cy-ay |
     * </pre>
     * @param a first vertex
     * @param b second vertex
     * @param c third vertex
     * @return area > 0 CCW, ..
     */
    public static double triArea(final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c){
        return triArea(a.x(), a.y(), b.x(), b.y(), c.x(), c.y());
    }
    private static double triArea(final double ax, final double ay, final double bx, final double by, final double cx, final double cy){
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    /**
     * Check if a vertex is in triangle using barycentric coordinates computation.
     * @param a first triangle vertex
     * @param b second triangle vertex
     * @param c third triangle vertex
     * @param p the vertex in question
     * @param ac temporary storage
     * @param ab temporary storage
     * @param ap temporary storage
     * @return true if p is in triangle (a, b, c), false otherwise.
     */
    public static boolean isInTriangle(final Vec3f a, final Vec3f  b, final Vec3f c,
                                       final Vec3f p,
                                       final Vec3f ac, final Vec3f ab, final Vec3f ap){
        // Compute vectors
        ac.minus( c, a); // v0
        ab.minus( b, a); // v1
        ap.minus( p, a); // v2

        // Compute dot products
        final float dotAC_AC = ac.dot(ac);
        final float dotAC_AB = ac.dot(ab);
        final float dotAB_AB = ab.dot(ab);
        final float dotAC_AP = ac.dot(ap);
        final float dotAB_AP = ab.dot(ap);

        // Compute barycentric coordinates
        final float invDenom = 1 / (dotAC_AC * dotAB_AB - dotAC_AB * dotAC_AB);
        final float u = (dotAB_AB * dotAC_AP - dotAC_AB * dotAB_AP) * invDenom;
        final float v = (dotAC_AC * dotAB_AP - dotAC_AB * dotAC_AP) * invDenom;

        // Check if point is in triangle
        return (u >= 0) && (v >= 0) && (u + v < 1);
    }

    /**
     * Check if one of three vertices are in triangle using barycentric coordinates computation.
     * @param a first triangle vertex
     * @param b second triangle vertex
     * @param c third triangle vertex
     * @param p1 the vertex in question
     * @param p2 the vertex in question
     * @param p3 the vertex in question
     * @param ac temporary storage
     * @param ab temporary storage
     * @param ap temporary storage
     * @return true if p1 or p2 or p3 is in triangle (a, b, c), false otherwise.
     */
    public static boolean isInTriangle3(final Vec3f a, final Vec3f b, final Vec3f c,
                                        final Vec3f p1, final Vec3f p2, final Vec3f p3,
                                        final Vec3f ac, final Vec3f ab, final Vec3f ap){
        // Compute vectors
        ac.minus(c, a); // v0
        ab.minus(b, a); // v1

        // Compute dot products
        final float dotAC_AC = ac.dot(ac);
        final float dotAC_AB = ac.dot(ab);
        final float dotAB_AB = ab.dot(ab);

        // Compute barycentric coordinates
        final float invDenom = 1 / (dotAC_AC * dotAB_AB - dotAC_AB * dotAC_AB);
        {
            ap.minus(p1, a); // v2
            final float dotAC_AP1 = ac.dot(ap);
            final float dotAB_AP1 = ab.dot(ap);
            final float u = (dotAB_AB * dotAC_AP1 - dotAC_AB * dotAB_AP1) * invDenom;
            final float v = (dotAC_AC * dotAB_AP1 - dotAC_AB * dotAC_AP1) * invDenom;

            // Check if point is in triangle
            if ( (u >= 0) && (v >= 0) && (u + v < 1) ) {
                return true;
            }
        }

        {
            ap.minus(p2, a); // v2
            final float dotAC_AP2 = ac.dot(ap);
            final float dotAB_AP2 = ab.dot(ap);
            final float u = (dotAB_AB * dotAC_AP2 - dotAC_AB * dotAB_AP2) * invDenom;
            final float v = (dotAC_AC * dotAB_AP2 - dotAC_AB * dotAC_AP2) * invDenom;

            // Check if point is in triangle
            if ( (u >= 0) && (v >= 0) && (u + v < 1) ) {
                return true;
            }
        }

        {
            ap.minus(p3, a); // v3
            final float dotAC_AP3 = ac.dot(ap);
            final float dotAB_AP3 = ab.dot(ap);
            final float u = (dotAB_AB * dotAC_AP3 - dotAC_AB * dotAB_AP3) * invDenom;
            final float v = (dotAC_AC * dotAB_AP3 - dotAC_AB * dotAC_AP3) * invDenom;

            // Check if point is in triangle
            if ( (u >= 0) && (v >= 0) && (u + v < 1) ) {
                return true;
            }
        }
        return false;
    }
    /**
     * Check if one of three vertices are in triangle using
     * barycentric coordinates computation, using given epsilon for comparison.
     * @param a first triangle vertex
     * @param b second triangle vertex
     * @param c third triangle vertex
     * @param p1 the vertex in question
     * @param p2 the vertex in question
     * @param p3 the vertex in question
     * @param tmpAC
     * @param tmpAB
     * @param tmpAP
     * @return true if p1 or p2 or p3 is in triangle (a, b, c), false otherwise.
     */
    public static boolean isInTriangle3(final Vec3f a, final Vec3f b, final Vec3f c,
                                        final Vec3f p1, final Vec3f p2, final Vec3f p3,
                                        final Vec3f ac, final Vec3f ab, final Vec3f ap,
                                        final float epsilon) {
        // Compute vectors
        ac.minus(c, a); // v0
        ab.minus(b, a); // v1

        // Compute dot products
        final float dotAC_AC = ac.dot(ac);
        final float dotAC_AB = ac.dot(ab);
        final float dotAB_AB = ab.dot(ab);

        // Compute barycentric coordinates
        final float invDenom = 1 / (dotAC_AC * dotAB_AB - dotAC_AB * dotAC_AB);
        {
            ap.minus(p1, a); // v2
            final float dotAC_AP1 = ac.dot(ap);
            final float dotAB_AP1 = ab.dot(ap);
            final float u = (dotAB_AB * dotAC_AP1 - dotAC_AB * dotAB_AP1) * invDenom;
            final float v = (dotAC_AC * dotAB_AP1 - dotAC_AB * dotAC_AP1) * invDenom;

            // Check if point is in triangle
            if( FloatUtil.compare(u, 0.0f, epsilon) >= 0 &&
                FloatUtil.compare(v, 0.0f, epsilon) >= 0 &&
                FloatUtil.compare(u+v, 1.0f, epsilon) < 0 ) {
                return true;
            }
        }

        {
            ap.minus(p2, a); // v3
            final float dotAC_AP2 = ac.dot(ap);
            final float dotAB_AP2 = ab.dot(ap);
            final float u = (dotAB_AB * dotAC_AP2 - dotAC_AB * dotAB_AP2) * invDenom;
            final float v = (dotAC_AC * dotAB_AP2 - dotAC_AB * dotAC_AP2) * invDenom;

            // Check if point is in triangle
            if( FloatUtil.compare(u, 0.0f, epsilon) >= 0 &&
                FloatUtil.compare(v, 0.0f, epsilon) >= 0 &&
                FloatUtil.compare(u+v, 1.0f, epsilon) < 0 ) {
                return true;
            }
        }

        {
            ap.minus(p3, a); // v4
            final float dotAC_AP3 = ac.dot(ap);
            final float dotAB_AP3 = ab.dot(ap);
            final float u = (dotAB_AB * dotAC_AP3 - dotAC_AB * dotAB_AP3) * invDenom;
            final float v = (dotAC_AC * dotAB_AP3 - dotAC_AB * dotAC_AP3) * invDenom;

            // Check if point is in triangle
            if( FloatUtil.compare(u, 0.0f, epsilon) >= 0 &&
                FloatUtil.compare(v, 0.0f, epsilon) >= 0 &&
                FloatUtil.compare(u+v, 1.0f, epsilon) < 0 ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if points are in ccw order
     * <p>
     * Consider using {@link #getWinding(List)} using the {@link #area(List)} function over all points
     * on complex shapes for a reliable result!
     * </p>
     * @param a first vertex
     * @param b second vertex
     * @param c third vertex
     * @return true if the points a,b,c are in a ccw order
     * @see #getWinding(List)
     */
    public static boolean isCCW(final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c){
        return triArea(a,b,c) > DoubleUtil.EPSILON;
    }

    /**
     * Compute the winding of the 3 given points
     * <p>
     * Consider using {@link #getWinding(List)} using the {@link #area(List)} function over all points
     * on complex shapes for a reliable result!
     * </p>
     * @param a first vertex
     * @param b second vertex
     * @param c third vertex
     * @return {@link Winding#CCW} or {@link Winding#CW}
     * @see #getWinding(List)
     */
    public static Winding getWinding(final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c) {
        return triArea(a,b,c) > DoubleUtil.EPSILON ? Winding.CCW : Winding.CW ;
    }

    /**
     * Computes the area of a list of vertices via shoelace formula.
     * <p>
     * This method is utilized e.g. to reliably compute the {@link Winding} of complex shapes.
     * </p>
     * <p>
     * Implementation uses double precision.
     * </p>
     * @param vertices
     * @return positive area if ccw else negative area value
     * @see #getWinding(List)
     */
    public static double area(final List<? extends Vert2fImmutable> vertices) {
        final int n = vertices.size();
        double area = 0.0;
        for (int p = n - 1, q = 0; q < n; p = q++) {
            final Vert2fImmutable pCoord = vertices.get(p);
            final Vert2fImmutable qCoord = vertices.get(q);
            area += (double)pCoord.x() * (double)qCoord.y() - (double)qCoord.x() * (double)pCoord.y();
        }
        return area;
    }

    /**
     * Compute the winding using the {@link #area(List)} function over all vertices for complex shapes.
     * <p>
     * Uses the {@link #area(List)} function over all points
     * on complex shapes for a reliable result!
     * </p>
     * <p>
     * Implementation uses double precision.
     * </p>
     * @param vertices array of Vertices
     * @return {@link Winding#CCW} or {@link Winding#CW}
     * @see #area(List)
     */
    public static Winding getWinding(final List<? extends Vert2fImmutable> vertices) {
        return area(vertices) >= 0 ? Winding.CCW : Winding.CW ;
    }

    /**
     * Finds the plane equation of a plane given its normal and a point on the plane.
     *
     * @param resultV4 vec4 plane equation
     * @param normalVec3
     * @param pVec3
     * @return result for chaining
     */
    public static Vec4f getPlaneVec3(final Vec4f resultV4, final Vec3f normalVec3, final Vec3f pVec3) {
        /**
            Ax + By + Cz + D == 0 ;
            D = - ( Ax + By + Cz )
              = - ( A*a[0] + B*a[1] + C*a[2] )
              = - vec3Dot ( normal, a ) ;
         */
        resultV4.set(normalVec3, -normalVec3.dot(pVec3));
        return resultV4;
    }

    /**
     * This finds the plane equation of a triangle given three vertices.
     *
     * @param resultVec4 vec4 plane equation
     * @param v1 vec3
     * @param v2 vec3
     * @param v3 vec3
     * @param temp1V3
     * @param temp2V3
     * @return result for chaining
     */
    public static Vec4f getPlaneVec3(final Vec4f resultVec4, final Vec3f v1, final Vec3f v2, final Vec3f v3,
                                     final Vec3f temp1V3, final Vec3f temp2V3, final Vec3f temp3V3) {
        /**
            Ax + By + Cz + D == 0 ;
            D = - ( Ax + By + Cz )
              = - ( A*a[0] + B*a[1] + C*a[2] )
              = - vec3Dot ( normal, a ) ;
         */
      temp3V3.cross(temp1V3.minus(v2, v1), temp2V3.minus(v3, v1)).normalize();
      resultVec4.set(temp3V3, -temp3V3.dot(v1));
      return resultVec4;
    }

    /**
     * Return intersection of an infinite line with a plane if exists, otherwise null.
     * <p>
     * Thanks to <i>Norman Vine -- nhv@yahoo.com  (with hacks by Steve)</i>
     * </p>
     *
     * @param result vec3 result buffer for intersecting coords
     * @param ray here representing an infinite line, origin and direction.
     * @param plane vec4 plane equation
     * @param epsilon
     * @return resulting intersecting if exists, otherwise null
     */
    public static Vec3f line2PlaneIntersection(final Vec3f result, final Ray ray, final Vec4f plane, final float epsilon) {
        final Vec3f plane3 = new Vec3f(plane);
        final float tmp = ray.dir.dot(plane3);

        if ( FloatUtil.isZero(tmp, epsilon) ) {
            return null; // ray is parallel to plane
        }
        result.set( ray.dir );
        return result.scale( -( ray.orig.dot(plane3) + plane.w() ) / tmp ).add(ray.orig);
    }

    /**
     * Compute intersection between two lines
     * @param a vertex 1 of first line
     * @param b vertex 2 of first line
     * @param c vertex 1 of second line
     * @param d vertex 2 of second line
     * @return the intersection coordinates if the lines intersect, otherwise
     * returns null
     * @deprecated analyze correctness
     */
    @Deprecated
    public static Vec3f line2lineIntersection0(final Vec3f result,
                                               final Vert2fImmutable a, final Vert2fImmutable b,
                                               final Vert2fImmutable c, final Vert2fImmutable d) {
        final float determinant = (a.x()-b.x())*(c.y()-d.y()) - (a.y()-b.y())*(c.x()-d.x());

        if (determinant == 0)
            return null;

        final float alpha = (a.x()*b.y()-a.y()*b.x());
        final float beta = (c.x()*d.y()-c.y()*d.y());
        final float xi = ((c.x()-d.x())*alpha-(a.x()-b.x())*beta)/determinant;
        final float yi = ((c.y()-d.y())*alpha-(a.y()-b.y())*beta)/determinant;

        return result.set(xi, yi, 0);
    }
    /** Compute intersection between two segments
     * @param a vertex 1 of first segment
     * @param b vertex 2 of first segment
     * @param c vertex 1 of second segment
     * @param d vertex 2 of second segment
     * @return the intersection coordinates if the segments intersect, otherwise returns null
     * @deprecated use {@link #seg2SegIntersection(Vec3f, Vec2f, Vec2f, Vec2f, Vec2f, boolean)
     */
    @Deprecated
    public static Vec3f seg2SegIntersection0(final Vec3f result, final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c, final Vert2fImmutable d) {
        final float determinant = (a.x()-b.x())*(c.y()-d.y()) - (a.y()-b.y())*(c.x()-d.x());

        if (determinant == 0)
            return null;

        final float alpha = (a.x()*b.y()-a.y()*b.x());
        final float beta = (c.x()*d.y()-c.y()*d.y());
        final float xi = ((c.x()-d.x())*alpha-(a.x()-b.x())*beta)/determinant;
        final float yi = ((c.y()-d.y())*alpha-(a.y()-b.y())*beta)/determinant;

        final float gamma = (xi - a.x())/(b.x() - a.x());
        final float gamma1 = (xi - c.x())/(d.x() - c.x());
        if(gamma <= 0 || gamma >= 1) return null;
        if(gamma1 <= 0 || gamma1 >= 1) return null;

        return result.set(xi, yi, 0);
    }
    /**
     * Compute intersection between two segments
     * @param a vertex 1 of first segment
     * @param b vertex 2 of first segment
     * @param c vertex 1 of second segment
     * @param d vertex 2 of second segment
     * @return true if the segments intersect, otherwise returns false
     * @deprecated use {@link #testSeg2SegIntersection(Vert2fImmutable, Vert2fImmutable, Vert2fImmutable, Vert2fImmutable, float, boolean)}
     */
    @Deprecated
    public static boolean testSeg2SegIntersection0(final Vert2fImmutable a, final Vert2fImmutable b,
                                                   final Vert2fImmutable c, final Vert2fImmutable d) {
        // Operations: 14+, 11*, 2 branches
        final float determinant = (a.x()-b.x())*(c.y()-d.y()) - (a.y()-b.y())*(c.x()-d.x());

        if (determinant == 0) {
            return false;
        }

        final float alpha = (a.x()*b.y()-a.y()*b.x());
        final float beta = (c.x()*d.y()-c.y()*d.y());
        final float xi = ((c.x()-d.x())*alpha-(a.x()-b.x())*beta)/determinant;

        final float gamma0 = (xi - a.x())/(b.x() - a.x());
        final float gamma1 = (xi - c.x())/(d.x() - c.x());
        if(gamma0 <= 0 || gamma0 >= 1 || gamma1 <= 0 || gamma1 >= 1) {
            return false;
        }

        return true;
    }
    /**
     * Check if a segment intersects with a triangle
     * @param a vertex 1 of the triangle
     * @param b vertex 2 of the triangle
     * @param c vertex 3 of the triangle
     * @param d vertex 1 of first segment
     * @param e vertex 2 of first segment
     * @return true if the segment intersects at least one segment of the triangle, false otherwise
     * @deprecated use {@link #testTri2SegIntersection(Vert2fImmutable, Vert2fImmutable, Vert2fImmutable, Vert2fImmutable, Vert2fImmutable, float, boolean)}
     */
    @Deprecated
    public static boolean testTri2SegIntersection0(final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c,
                                                   final Vert2fImmutable d, final Vert2fImmutable e){
        return testSeg2SegIntersection0(a, b, d, e) ||
               testSeg2SegIntersection0(b, c, d, e) ||
               testSeg2SegIntersection0(a, c, d, e) ;
    }

    /**
     * Line segment intersection test and returning the intersecting point.
     * <p>
     * See [p + t r = q + u s](https://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect/565282#565282)
     * and [its terse C# implementation](https://www.codeproject.com/tips/862988/find-the-intersection-point-of-two-line-segments)
     * </p>
     * <p>
     * Implementation uses float precision.
     * </p>
     * @param p vertex 1 of first segment
     * @param p2 vertex 2 of first segment
     * @param q vertex 1 of second segment
     * @param q2 vertex 2 of second segment
     * @param epsilon optional epsilon. If {@code 0} just compare against {@code 0}, otherwise compare against this {@code epsilon}. See {@link FloatUtil#EPSILON}
     * @param doCollinear consider collinear case, i.e. overlapping segments as an intersection returning {@code true}
     * @return true if line segments are intersecting, otherwise false
     */
    public static boolean seg2SegIntersection(final Vec3f result, final Vec2f p, final Vec2f p2, final Vec2f q, final Vec2f q2, final float epsilon, final boolean doCollinear)
    {
        final Vec2f r = p2.minus(p);
        final Vec2f s = q2.minus(q);
        final float rxs = r.cross(s);

        if ( FloatUtil.isZero(rxs, epsilon) ) {
            if( doCollinear ) {
                final Vec2f q_p = q.minus(p);
                final float qpxr = q_p.cross(r);
                if ( FloatUtil.isZero(qpxr, epsilon) ) // disabled collinear case
                {
                    // 1) r x s = 0 and (q - p) x r = 0, the two lines are collinear.
                    final Vec2f p_q = p.minus(q);
                    final float qp_dot_r = q_p.dot(r);
                    final float pq_dot_s = p_q.dot(s);
                    // if ( ( 0 <= qp_dot_r && qp_dot_r <= r.dot(r) ) ||
                    //      ( 0 <= pq_dot_s && pq_dot_s <= s.dot(s) ) )
                    if ( ( epsilon <= qp_dot_r && qp_dot_r - r.dot(r) <= epsilon ) ||
                         ( epsilon <= pq_dot_s && pq_dot_s - s.dot(s) <= epsilon ) )
                    {
                        // 1.1) 0 <= (q - p) · r <= r · r or 0 <= (p - q) · s <= s · s, the two lines are overlapping
                        // FIXME: result set to q2 endpoint, OK?
                        result.set(q2, 0);
                        return true;
                    }
                    // 1.2 the two lines are collinear but disjoint.
                    return false;
                } else {
                    // 2) r × s = 0 and (q − p) × r ≠ 0, the two lines are parallel and non-intersecting.
                    return false;
                }
            } else {
                // Not considering collinear case as an intersection
                return false;
            }
        } else {
            // r x s != 0
            final Vec2f q_p = q.minus(p);
            final float qpxr = q_p.cross(r);

            // p + t r = q + u s
            // (p + t r) × s = (q + u s) × s
            // t (r × s) = (q − p) × s, with s x s = 0
            // t = (q - p) x s / (r x s)
            final float t = q_p.cross(s) / rxs;

            // u = (p − q) × r / (s × r) = (q - p) x r / (r x s), with s × r = − r × s
            final float u = qpxr / rxs;

            // if ( (0 <= t && t <= 1) && (0 <= u && u <= 1) )
            if ( (epsilon <= t && t - 1 <= epsilon) && (epsilon <= u && u - 1 <= epsilon) )
            {
                // 3) r × s ≠ 0 and 0 ≤ t ≤ 1 and 0 ≤ u ≤ 1, the two line segments meet at the point p + t * r = q + u * s.
                result.set( p.plus( r.mul( t ) ), 0 ); // == q + (u * s)
                return true;
            }
        }

        return false;
    }
    /**
     * Line segment intersection test.
     * <p>
     * See [p + t r = q + u s](https://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect/565282#565282)
     * and [its terse C# implementation](https://www.codeproject.com/tips/862988/find-the-intersection-point-of-two-line-segments)
     * </p>
     * <p>
     * Implementation uses float precision.
     * </p>
     * @param p vertex 1 of first segment
     * @param p2 vertex 2 of first segment
     * @param q vertex 1 of second segment
     * @param q2 vertex 2 of second segment
     * @param epsilon optional epsilon. If {@code 0} just compare against {@code 0}, otherwise compare against this {@code epsilon}. See {@link FloatUtil#EPSILON}
     * @param doCollinear consider collinear case, i.e. overlapping segments as an intersection returning {@code true}
     * @return true if line segments are intersecting, otherwise false
     */
    public static boolean testSeg2SegIntersection(final Vec2f p, final Vec2f p2, final Vec2f q, final Vec2f q2, final float epsilon, final boolean doCollinear)
    {
        final Vec2f r = p2.minus(p);
        final Vec2f s = q2.minus(q);
        final float rxs = r.cross(s);

        if ( FloatUtil.isZero(rxs, epsilon) ) {
            if( doCollinear ) {
                final Vec2f q_p = q.minus(p);
                final float qpxr = q_p.cross(r);
                if ( FloatUtil.isZero(qpxr, epsilon) ) // disabled collinear case
                {
                    // 1) r x s = 0 and (q - p) x r = 0, the two lines are collinear.
                    final Vec2f p_q = p.minus(q);
                    final float qp_dot_r = q_p.dot(r);
                    final float pq_dot_s = p_q.dot(s);
                    // if ( ( 0 <= qp_dot_r && qp_dot_r <= r.dot(r) ) ||
                    //      ( 0 <= pq_dot_s && pq_dot_s <= s.dot(s) ) )
                    if ( ( epsilon <= qp_dot_r && qp_dot_r - r.dot(r) <= epsilon ) ||
                         ( epsilon <= pq_dot_s && pq_dot_s - s.dot(s) <= epsilon ) )
                    {
                        // 1.1) 0 <= (q - p) · r <= r · r or 0 <= (p - q) · s <= s · s, the two lines are overlapping
                        return true;
                    }
                    // 1.2 the two lines are collinear but disjoint.
                    return false;
                } else {
                    // 2) r × s = 0 and (q − p) × r ≠ 0, the two lines are parallel and non-intersecting.
                    return false;
                }
            } else {
                // Not considering collinear case as an intersection
                return false;
            }
        } else {
            // r x s != 0
            final Vec2f q_p = q.minus(p);
            final float qpxr = q_p.cross(r);

            // p + t r = q + u s
            // (p + t r) × s = (q + u s) × s
            // t (r × s) = (q − p) × s, with s x s = 0
            // t = (q - p) x s / (r x s)
            final float t = q_p.cross(s) / rxs;

            // u = (p − q) × r / (s × r) = (q - p) x r / (r x s), with s × r = − r × s
            final float u = qpxr / rxs;

            // if ( (0 <= t && t <= 1) && (0 <= u && u <= 1) )
            if ( (epsilon <= t && t - 1 <= epsilon) && (epsilon <= u && u - 1 <= epsilon) )
            {
                // 3) r × s ≠ 0 and 0 ≤ t ≤ 1 and 0 ≤ u ≤ 1, the two line segments meet at the point p + t * r = q + u * s.
                return true;
            }
        }
        return false;
    }
    /**
     * Line segment intersection test
     * <p>
     * See [p + t r = q + u s](https://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect/565282#565282)
     * and [its terse C# implementation](https://www.codeproject.com/tips/862988/find-the-intersection-point-of-two-line-segments)
     * </p>
     * <p>
     * Implementation uses float precision.
     * </p>
     * @param p vertex 1 of first segment
     * @param p2 vertex 2 of first segment
     * @param q vertex 1 of second segment
     * @param q2 vertex 2 of second segment
     * @param epsilon optional epsilon. If {@code 0} just compare against {@code 0}, otherwise compare against this {@code epsilon}. See {@link FloatUtil#EPSILON}
     * @param doCollinear consider collinear case, i.e. overlapping segments as an intersection returning {@code true}
     * @return true if line segments are intersecting, otherwise false
     */
    public static boolean testSeg2SegIntersection(final Vert2fImmutable p, final Vert2fImmutable p2, final Vert2fImmutable q, final Vert2fImmutable q2,
                                                  final float epsilon, final boolean doCollinear)
    {
        // Operations: 11+, 8*, 2 branches
        final float rx = p2.x() - p.x(); // p2.minus(p)
        final float ry = p2.y() - p.y();
        final float sx = q2.x() - q.x(); // q2.minus(q)
        final float sy = q2.y() - q.y();
        final float rxs = rx * sy - ry * sx; // r.cross(s)

        if ( FloatUtil.isZero(rxs, epsilon) ) {
            if( doCollinear ) {
                final float q_px = q.x() - p.x(); // q.minus(p)
                final float q_py = q.y() - p.y();
                final float qpxr = q_px * ry - q_py * rx; // q_p.cross(r)
                if ( FloatUtil.isZero(qpxr, epsilon) ) // disabled collinear case
                {
                    // 1) r x s = 0 and (q - p) x r = 0, the two lines are collinear.
                    final float p_qx = p.x() - q.x(); // p.minus(q)
                    final float p_qy = p.y() - q.y();
                    final float qp_dot_r = q_px * rx + q_py * ry; // q_p.dot(r);
                    final float pq_dot_s = p_qx * sx + p_qy * sy; // p_q.dot(s);
                    final float r_dot_r = rx * rx + ry * ry; // r.dot(r);
                    final float s_dot_s = sx * sx + sy * sy; // r.dot(r);
                    // if ( ( 0 <= qp_dot_r && qp_dot_r <= r.dot(r) ) ||
                    //      ( 0 <= pq_dot_s && pq_dot_s <= s.dot(s) ) )
                    if ( ( epsilon <= qp_dot_r && qp_dot_r - r_dot_r <= epsilon ) ||
                         ( epsilon <= pq_dot_s && pq_dot_s - s_dot_s <= epsilon ) )
                    {
                        // 1.1) 0 <= (q - p) · r <= r · r or 0 <= (p - q) · s <= s · s, the two lines are overlapping
                        return true;
                    }
                    // 1.2 the two lines are collinear but disjoint.
                    return false;
                } else {
                    // 2) r × s = 0 and (q − p) × r ≠ 0, the two lines are parallel and non-intersecting.
                    return false;
                }
            } else {
                // Not considering collinear case as an intersection
                return false;
            }
        } else {
            // r x s != 0
            final float q_px = q.x() - p.x(); // q.minus(p)
            final float q_py = q.y() - p.y();
            final float qpxr = q_px * ry - q_py * rx; // q_p.cross(r)

            // p + t r = q + u s
            // (p + t r) × s = (q + u s) × s
            // t (r × s) = (q − p) × s, with s x s = 0
            // t = (q - p) x s / (r x s)
            final float t = ( q_px * sy - q_py * sx ) / rxs; // q_p.cross(s) / rxs

            // u = (p − q) × r / (s × r) = (q - p) x r / (r x s), with s × r = − r × s
            final float u = qpxr / rxs;

            // if ( (0 <= t && t <= 1) && (0 <= u && u <= 1) )
            if ( (epsilon <= t && t - 1 <= epsilon) && (epsilon <= u && u - 1 <= epsilon) )
            {
                // 3) r × s ≠ 0 and 0 ≤ t ≤ 1 and 0 ≤ u ≤ 1, the two line segments meet at the point p + t * r = q + u * s.
                return true;
            }
        }
        return false;
    }
    /**
     * Check if a segment intersects with a triangle.
     * <p>
     * Implementation uses float precision.
     * </p>
     * @param a vertex 1 of the triangle
     * @param b vertex 2 of the triangle
     * @param c vertex 3 of the triangle
     * @param d vertex 1 of first segment
     * @param e vertex 2 of first segment
     * @param epsilon optional epsilon. If {@code 0} just compare against {@code 0}, otherwise compare against this {@code epsilon}. See {@link FloatUtil#EPSILON}
     * @param doCollinear consider collinear case, i.e. overlapping segments as an intersection returning {@code true}
     * @return true if the segment intersects at least one segment of the triangle, false otherwise
     * @see #testSeg2SegIntersection(Vert2fImmutable, Vert2fImmutable, Vert2fImmutable, Vert2fImmutable, float, boolean)
     */
    public static boolean testTri2SegIntersection(final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c,
                                                  final Vert2fImmutable d, final Vert2fImmutable e, final float epsilon, final boolean doCollinear) {
        return testSeg2SegIntersection(a, b, d, e, epsilon, doCollinear) ||
               testSeg2SegIntersection(b, c, d, e, epsilon, doCollinear) ||
               testSeg2SegIntersection(a, c, d, e, epsilon, doCollinear) ;
    }

    /**
     * Line segment intersection test using {@link FloatUtil#EPSILON} w/o considering collinear-case
     * <p>
     * See [p + t r = q + u s](https://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect/565282#565282)
     * and [its terse C# implementation](https://www.codeproject.com/tips/862988/find-the-intersection-point-of-two-line-segments)
     * </p>
     * <p>
     * Implementation uses float precision.
     * </p>
     * @param p vertex 1 of first segment
     * @param p2 vertex 2 of first segment
     * @param q vertex 1 of second segment
     * @param q2 vertex 2 of second segment
     * @return true if line segments are intersecting, otherwise false
     */
    public static boolean testSeg2SegIntersection(final Vert2fImmutable p, final Vert2fImmutable p2, final Vert2fImmutable q, final Vert2fImmutable q2)
    {
        // Operations: 11+, 8*, 2 branches
        final float rx = p2.x() - p.x(); // p2.minus(p)
        final float ry = p2.y() - p.y();
        final float sx = q2.x() - q.x(); // q2.minus(q)
        final float sy = q2.y() - q.y();
        final float rxs = rx * sy - ry * sx; // r.cross(s)

        final float epsilon = FloatUtil.EPSILON;

        if ( FloatUtil.isZero(rxs) ) {
            // Not considering collinear case as an intersection
            return false;
        } else {
            // r x s != 0
            final float q_px = q.x() - p.x(); // q.minus(p)
            final float q_py = q.y() - p.y();
            final float qpxr = q_px * ry - q_py * rx; // q_p.cross(r)

            // p + t r = q + u s
            // (p + t r) × s = (q + u s) × s
            // t (r × s) = (q − p) × s, with s x s = 0
            // t = (q - p) x s / (r x s)
            final float t = ( q_px * sy - q_py * sx ) / rxs; // q_p.cross(s) / rxs

            // u = (p − q) × r / (s × r) = (q - p) x r / (r x s), with s × r = − r × s
            final float u = qpxr / rxs;

            // if ( (0 <= t && t <= 1) && (0 <= u && u <= 1) )
            if ( (epsilon <= t && t - 1 <= epsilon) && (epsilon <= u && u - 1 <= epsilon) )
            {
                // 3) r × s ≠ 0 and 0 ≤ t ≤ 1 and 0 ≤ u ≤ 1, the two line segments meet at the point p + t * r = q + u * s.
                return true;
            }
        }
        return false;
    }
    /**
     * Check if a segment intersects with a triangle using {@link FloatUtil#EPSILON} w/o considering collinear-case
     * <p>
     * Implementation uses float precision.
     * </p>
     * @param a vertex 1 of the triangle
     * @param b vertex 2 of the triangle
     * @param c vertex 3 of the triangle
     * @param d vertex 1 of first segment
     * @param e vertex 2 of first segment
     * @return true if the segment intersects at least one segment of the triangle, false otherwise
     * @see #testSeg2SegIntersection(Vert2fImmutable, Vert2fImmutable, Vert2fImmutable, Vert2fImmutable, float, boolean)
     */
    public static boolean testTri2SegIntersection(final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c,
                                                  final Vert2fImmutable d, final Vert2fImmutable e) {
        return testSeg2SegIntersection(a, b, d, e) ||
               testSeg2SegIntersection(b, c, d, e) ||
               testSeg2SegIntersection(a, c, d, e) ;
    }

    /**
     * Returns whether the given {@code polyline} denotes a convex shape with O(n) complexity.
     * <p>
     * See [Determine whether a polygon is convex based on its vertices](https://math.stackexchange.com/questions/1743995/determine-whether-a-polygon-is-convex-based-on-its-vertices/1745427#1745427)
     * </p>
     * @param polyline connected {@link Vert2fImmutable}, i.e. a poly-line
     * @param shortIsConvex return value if {@code vertices} have less than three elements, allows simplification
     */
    public static boolean isConvex0(final List<? extends Vert2fImmutable> polyline, final boolean shortIsConvex) {
        final int polysz = polyline.size();
        if( polysz < 3 ) {
            return shortIsConvex;
        }
        final float eps = FloatUtil.EPSILON;

        float wSign = 0;    // First nonzero orientation (positive or negative)

        int xSign = 0;
        int xFirstSign = 0; //  Sign of first nonzero edge vector x
        int xFlips = 0;     //  Number of sign changes in x

        int ySign = 0;
        int yFirstSign = 0; //  Sign of first nonzero edge vector y
        int yFlips = 0;     //  Number of sign changes in y

        Vert2fImmutable curr = polyline.get(polysz-2); //  Second-to-last vertex
        Vert2fImmutable next = polyline.get(polysz-1); //  Last vertex

        for(int i=0; i<polysz; ++i) { //  Each vertex, in order
            final Vert2fImmutable prev = curr; //  Previous vertex
            curr = next;              //  Current vertex
            next = polyline.get(i);   //  Next vertex

            //  Previous edge vector ("before"):
            final float bx = curr.x() - prev.x();
            final float by = curr.y() - prev.y();

            //  Next edge vector ("after"):
            final float ax = next.x() - curr.x();
            final float ay = next.y() - curr.y();

            //  Calculate sign flips using the next edge vector ("after"),
            //  recording the first sign.
            if( ax > eps ) {
                if( xSign == 0 ) {
                    xFirstSign = +1;
                } else if( xSign < 0 ) {
                    xFlips = xFlips + 1;
                }
                xSign = +1;
            } else if( ax < -eps ) {
                if( xSign == 0 ) {
                    xFirstSign = -1;
                } else if ( xSign > 0 ) {
                    xFlips = xFlips + 1;
                }
                xSign = -1;
            }
            if( xFlips > 2 ) {
                return false;
            }

            if( ay > eps ) {
                if( ySign == 0 ) {
                    yFirstSign = +1;
                } else if( ySign < 0 ) {
                    yFlips = yFlips + 1;
                }
                ySign = +1;
            } else if( ay < -eps ) {
                if( ySign == 0 ) {
                    yFirstSign = -1;
                } else if( ySign > 0 ) {
                    yFlips = yFlips + 1;
                }
                ySign = -1;
            }
            if( yFlips > 2 ) {
                return false;
            }

            //  Find out the orientation of this pair of edges,
            //  and ensure it does not differ from previous ones.
            final float w = bx*ay - ax*by;
            if( FloatUtil.isZero(wSign) && !FloatUtil.isZero(w) ) {
                wSign = w;
            } else if( wSign > eps && w < -eps ) {
                return false;
            } else if( wSign < -eps && w > eps ) {
                return false;
            }
        }

        //  Final/wraparound sign flips:
        if( xSign != 0 && xFirstSign != 0 && xSign != xFirstSign ) {
            xFlips = xFlips + 1;
        }
        if( ySign != 0 && yFirstSign != 0 && ySign != yFirstSign ) {
            yFlips = yFlips + 1;
        }

        //  Concave polygons have two sign flips along each axis.
        if( xFlips != 2 || yFlips != 2 ) {
            return false;
        }

        //  This is a convex polygon.
        return true;
    }


    private static int cmod(final int i, final int count) {
        if( i >= 0 ) {
            return i % count;
        } else {
            return i + count;
        }
    }
    /**
     * Returns whether the given on-curve {@code polyline} points denotes a convex shape with O(n) complexity.
     * <p>
     * See [Determine whether a polygon is convex based on its vertices](https://math.stackexchange.com/questions/1743995/determine-whether-a-polygon-is-convex-based-on-its-vertices/1745427#1745427)
     * </p>
     * <p>
     * All off-curve points are ignored.
     * </p>
     * @param polyline connected {@link Vert2fImmutable}, i.e. a poly-line
     * @param shortIsConvex return value if {@code vertices} have less than three elements, allows simplification
     */
    public static boolean isConvex1(final List<Vertex> polyline, final boolean shortIsConvex) {
        final int polysz = polyline.size();
        if( polysz < 3 ) {
            return shortIsConvex;
        }
        final float eps = FloatUtil.EPSILON;

        float wSign = 0;    // First nonzero orientation (positive or negative)

        int xSign = 0;
        int xFirstSign = 0; //  Sign of first nonzero edge vector x
        int xFlips = 0;     //  Number of sign changes in x

        int ySign = 0;
        int yFirstSign = 0; //  Sign of first nonzero edge vector y
        int yFlips = 0;     //  Number of sign changes in y

        int offset=-3;
        Vertex v0, v1;
        {
            do {
                ++offset; // -2
                v0 = polyline.get(cmod(offset, polysz));   // current, polyline[-2] if on-curve
            } while( !v0.isOnCurve() && offset < polysz );
            if( offset >= polysz ) {
                return shortIsConvex;
            }
            do {
                ++offset; // -1
                v1 = polyline.get(cmod(offset, polysz));   //  next, polyline[-1] if both on-curve
            } while( !v1.isOnCurve() && offset < polysz );
            if( offset >= polysz ) {
                return shortIsConvex;
            }
        }

        while( offset < polysz ) {
            final Vertex vp = v0;                         //  previous on-curve vertex
            v0 = v1;                                      //  current on-curve vertex
            do {
                ++offset; // 0, ...
                v1 = polyline.get(cmod(offset, polysz));  //  next on-curve vertex
            } while( !v1.isOnCurve() && offset < polysz );
            if( offset >= polysz ) {
                break;
            }

            //  Previous edge vector ("before"):
            final float bx = v0.x() - vp.x();
            final float by = v0.y() - vp.y();

            //  Next edge vector ("after"):
            final float ax = v1.x() - v0.x();
            final float ay = v1.y() - v0.y();

            //  Calculate sign flips using the next edge vector ("after"),
            //  recording the first sign.
            if( ax > eps ) {
                if( xSign == 0 ) {
                    xFirstSign = +1;
                } else if( xSign < 0 ) {
                    xFlips = xFlips + 1;
                }
                xSign = +1;
            } else if( ax < -eps ) {
                if( xSign == 0 ) {
                    xFirstSign = -1;
                } else if ( xSign > 0 ) {
                    xFlips = xFlips + 1;
                }
                xSign = -1;
            }
            if( xFlips > 2 ) {
                return false;
            }

            if( ay > eps ) {
                if( ySign == 0 ) {
                    yFirstSign = +1;
                } else if( ySign < 0 ) {
                    yFlips = yFlips + 1;
                }
                ySign = +1;
            } else if( ay < -eps ) {
                if( ySign == 0 ) {
                    yFirstSign = -1;
                } else if( ySign > 0 ) {
                    yFlips = yFlips + 1;
                }
                ySign = -1;
            }
            if( yFlips > 2 ) {
                return false;
            }

            //  Find out the orientation of this pair of edges,
            //  and ensure it does not differ from previous ones.
            final float w = bx*ay - ax*by;
            if( FloatUtil.isZero(wSign) && !FloatUtil.isZero(w) ) {
                wSign = w;
            } else if( wSign > eps && w < -eps ) {
                return false;
            } else if( wSign < -eps && w > eps ) {
                return false;
            }
        }

        //  Final/wraparound sign flips:
        if( xSign != 0 && xFirstSign != 0 && xSign != xFirstSign ) {
            xFlips = xFlips + 1;
        }
        if( ySign != 0 && yFirstSign != 0 && ySign != yFirstSign ) {
            yFlips = yFlips + 1;
        }

        //  Concave polygons have two sign flips along each axis.
        if( xFlips != 2 || yFlips != 2 ) {
            return false;
        }

        //  This is a convex polygon.
        return true;
    }

    /**
     * Returns whether the given {@code polyline} is self-intersecting shape (crossing over) with O(n*n) complexity.
     */
    public static boolean isSelfIntersecting1(final List<Vertex> polyline) {
        final int polysz = polyline.size();
        if (polysz < 4) {
            return false;
        }
        final boolean keepOffCurve = true;

        for (int i = 0; i < polysz-1; i++) {
            Vertex p0, p1;
            if( keepOffCurve ) {
                p0 = polyline.get(i);
                p1 = polyline.get(i+1);
            } else {
                do {
                    p0 = polyline.get(i++);
                } while( !p0.isOnCurve() && i < polysz-1 );
                if( i >= polysz-1 ) {
                    return false;
                }
                {
                    int i2 = i;
                    do {
                        p1 = polyline.get(i2++);
                    } while( !p1.isOnCurve() && i2 < polysz );
                    i = i2 - 1;
                    if( i2 >= polysz ) {
                        return false;
                    }
                }
            }
            for (int j = i+2; j < polysz; j++) {
                // Eliminate combinations already checked or not valid
                if ((i == 0) && ( j == (polysz-1))) {
                    continue;
                }
                Vertex q0, q1;
                if( keepOffCurve ) {
                    q0 = polyline.get(j);
                    q1 = polyline.get((j+1)%polysz);
                } else {
                    do {
                        q0 = polyline.get(j++);
                    } while( !q0.isOnCurve() && j < polysz );
                    if( i >= polysz-1 ) {
                        return false;
                    }
                    {
                        int j2 = j;
                        do {
                            q1 = polyline.get(j2++%polysz);
                        } while( !q1.isOnCurve() && j2 < polysz+1 );
                        j = j2 - 1;
                        if( j2 >= polysz+1 ) {
                            return false;
                        }
                    }
                }

                if( VectorUtil.testSeg2SegIntersection(p0, p1, q0, q1, 0, false) ) {
                    return true;
                }
            }
        }
        return false;
    }
}
