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
package com.jogamp.opengl.math;

import java.util.ArrayList;

public class VectorUtil {

    public static final float[] VEC3_ONE = { 1f, 1f, 1f };
    public static final float[] VEC3_ZERO = { 0f, 0f, 0f };

    public enum Winding {
        CW(-1), CCW(1);

        public final int dir;

        Winding(int dir) {
            this.dir = dir;
        }
    }

    /**
     * Copies a vector of length 3
     * @param dst output vector
     * @param dstOffset offset of dst in array
     * @param src input vector
     * @param srcOffset offset of src in array
     * @return copied output vector for chaining
     */
    public static float[] copyVec3(final float[] dst, int dstOffset, final float[] src, int srcOffset)
    {
        System.arraycopy(src, srcOffset, dst, dstOffset, 3);
        return dst;
    }

    /**
     * Copies a vector of length 4
     * @param dst output vector
     * @param dstOffset offset of dst in array
     * @param src input vector
     * @param srcOffset offset of src in array
     * @return copied output vector for chaining
     */
    public static float[] copyVec4(final float[] dst, int dstOffset, final float[] src, int srcOffset)
    {
        System.arraycopy(src, srcOffset, dst, dstOffset, 4);
        return dst;
    }

    /**
     * Return true if both vectors are equal w/o regarding an epsilon.
     * <p>
     * Implementation uses {@link FloatUtil#isEqual(float, float)}, see API doc for details.
     * </p>
     */
    public static boolean isVec2Equal(final float[] vec1, int vec1Offset, final float[] vec2, int vec2Offset) {
        return FloatUtil.isEqual(vec1[0+vec1Offset], vec2[0+vec2Offset]) &&
               FloatUtil.isEqual(vec1[1+vec1Offset], vec2[1+vec2Offset]) ;
    }

    /**
     * Return true if both vectors are equal w/o regarding an epsilon.
     * <p>
     * Implementation uses {@link FloatUtil#isEqual(float, float)}, see API doc for details.
     * </p>
     */
    public static boolean isVec3Equal(final float[] vec1, int vec1Offset, final float[] vec2, int vec2Offset) {
        return FloatUtil.isEqual(vec1[0+vec1Offset], vec2[0+vec2Offset]) &&
               FloatUtil.isEqual(vec1[1+vec1Offset], vec2[1+vec2Offset]) &&
               FloatUtil.isEqual(vec1[2+vec1Offset], vec2[2+vec2Offset]) ;
    }

    /**
     * Return true if both vectors are equal, i.e. their absolute delta < <code>epsilon</code>.
     * <p>
     * Implementation uses {@link FloatUtil#isEqual(float, float, float)}, see API doc for details.
     * </p>
     */
    public static boolean isVec2Equal(final float[] vec1, int vec1Offset, final float[] vec2, int vec2Offset, final float epsilon) {
        return FloatUtil.isEqual(vec1[0+vec1Offset], vec2[0+vec2Offset], epsilon) &&
               FloatUtil.isEqual(vec1[1+vec1Offset], vec2[1+vec2Offset], epsilon) ;
    }

    /**
     * Return true if both vectors are equal, i.e. their absolute delta < <code>epsilon</code>.
     * <p>
     * Implementation uses {@link FloatUtil#isEqual(float, float, float)}, see API doc for details.
     * </p>
     */
    public static boolean isVec3Equal(final float[] vec1, int vec1Offset, final float[] vec2, int vec2Offset, final float epsilon) {
        return FloatUtil.isEqual(vec1[0+vec1Offset], vec2[0+vec2Offset], epsilon) &&
               FloatUtil.isEqual(vec1[1+vec1Offset], vec2[1+vec2Offset], epsilon) &&
               FloatUtil.isEqual(vec1[2+vec1Offset], vec2[2+vec2Offset], epsilon) ;
    }

    /**
     * Return true if vector is zero, no {@link FloatUtil#EPSILON} is taken into consideration.
     */
    public static boolean isVec3Zero(final float[] vec, final int vecOffset) {
        return 0f == vec[0+vecOffset] && 0f == vec[1+vecOffset] && 0f == vec[2+vecOffset];
    }

    /**
     * Return true if vector is zero, i.e. it's absolute components < <code>epsilon</code>.
     * <p>
     * Implementation uses {@link FloatUtil#isZero(float, float)}, see API doc for details.
     * </p>
     */
    public static boolean isVec3Zero(final float[] vec, final int vecOffset, final float epsilon) {
        return isZero(vec[0+vecOffset], vec[1+vecOffset], vec[2+vecOffset], epsilon);
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
     * Return the squared distance between the given two points described vector v1 and v2.
     * <p>
     * When comparing the relative distance between two points it is usually sufficient to compare the squared
     * distances, thus avoiding an expensive square root operation.
     * </p>
     */
    public static float vec3DistanceSquare(final float[] v1, final float[] v2) {
        final float dx = v1[0] - v2[0];
        final float dy = v1[1] - v2[1];
        final float dz = v1[2] - v2[2];
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Return the distance between the given two points described vector v1 and v2.
     */
    public static float vec3Distance(final float[] v1, final float[] v2) {
        return FloatUtil.sqrt(vec3DistanceSquare(v1, v2));
    }

    /**
     * Compute the dot product of two points
     * @param vec1 vector 1
     * @param vec2 vector 2
     * @return the dot product as float
     */
    public static float vec3Dot(final float[] vec1, final float[] vec2)
    {
        return vec1[0]*vec2[0] + vec1[1]*vec2[1] + vec1[2]*vec2[2];
    }

    /**
     * Compute the squared length of a vector, a.k.a the squared <i>norm</i>
     */
    public static float vec3LengthSquare(final float[] vec) {
        return vec[0]*vec[0] + vec[1]*vec[1] + vec[2]*vec[2];
    }
    /**
     * Compute the length of a vector, a.k.a the <i>norm</i>
     */
    public static float vec3Length(final float[] vec) {
        return FloatUtil.sqrt(vec3LengthSquare(vec));
    }

    /**
     * Normalize a vector
     * @param result output vector, may be vector (in-place)
     * @param vector input vector
     * @return normalized output vector
     * @return result vector for chaining
     */
    public static float[] normalizeVec3(final float[] result, final float[] vector) {
        final float lengthSq = vec3LengthSquare(vector);
        if ( FloatUtil.isZero(lengthSq, FloatUtil.EPSILON) ) {
            result[0] = 0f;
            result[1] = 0f;
            result[2] = 0f;
        } else {
            final float invSqr = 1f / FloatUtil.sqrt(lengthSq);
            result[0] = vector[0] * invSqr;
            result[1] = vector[1] * invSqr;
            result[2] = vector[2] * invSqr;
        }
        return result;
    }

    /**
     * Normalize a vector in place
     * @param vector input vector
     * @return normalized output vector
     */
    public static float[] normalizeVec3(final float[] vector) {
        final float lengthSq = vec3LengthSquare(vector);
        if ( FloatUtil.isZero(lengthSq, FloatUtil.EPSILON) ) {
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
     * Scales a vector by param using given result float[]
     * @param result vector for the result, may be vector (in-place)
     * @param vector input vector
     * @param scale single scale constant for all vector components
     * @return result vector for chaining
     */
    public static float[] scaleVec3(final float[] result, final float[] vector, final float scale) {
        result[0] = vector[0] * scale;
        result[1] = vector[1] * scale;
        result[2] = vector[2] * scale;
        return result;
    }

    /**
     * Scales a vector by param using given result float[]
     * @param result vector for the result, may be vector (in-place)
     * @param vector input vector
     * @param scale 3 component scale constant for each vector component
     * @return result vector for chaining
     */
    public static float[] scaleVec3(final float[] result, final float[] vector, final float[] scale)
    {
        result[0] = vector[0] * scale[0];
        result[1] = vector[1] * scale[1];
        result[2] = vector[2] * scale[2];
        return result;
    }

    /**
     * Adds two vectors, result = v1 + v2
     * @param result float[3] result vector, may be either v1 or v2 (in-place)
     * @param v1 vector 1
     * @param v2 vector 2
     * @return result vector for chaining
     */
    public static float[] addVec3(final float[] result, final float[] v1, final float[] v2) {
        result[0] = v1[0] + v2[0];
        result[1] = v1[1] + v2[1];
        result[2] = v1[2] + v2[2];
        return result;
    }

    /**
     * Subtracts two vectors, result = v1 - v2
     * @param result float[3] result vector, may be either v1 or v2 (in-place)
     * @param v1 vector 1
     * @param v2 vector 2
     * @return result vector for chaining
     */
    public static float[] subVec3(final float[] result, final float[] v1, final float[] v2) {
        result[0] = v1[0] - v2[0];
        result[1] = v1[1] - v2[1];
        result[2] = v1[2] - v2[2];
        return result;
    }

    /**
     * cross product vec1 x vec2
     * @param v1 vector 1
     * @param v2 vector 2
     * @return the resulting vector
     */
    public static float[] crossVec3(final float[] result, final float[] v1, final float[] v2)
    {
        result[0] = v1[1] * v2[2] - v1[2] * v2[1];
        result[1] = v1[2] * v2[0] - v1[0] * v2[2];
        result[2] = v1[0] * v2[1] - v1[1] * v2[0];
        return result;
    }

    /**
     * Multiplication of column-major 4x4 matrix with vector
     * @param colMatrix column matrix (4x4)
     * @param vec vector(x,y,z)
     * @return result
     */
    public static float[] mulColMat4Vec3(final float[] result, final float[] colMatrix, final float[] vec)
    {
        result[0] = vec[0]*colMatrix[0] + vec[1]*colMatrix[4] + vec[2]*colMatrix[8] + colMatrix[12];
        result[1] = vec[0]*colMatrix[1] + vec[1]*colMatrix[5] + vec[2]*colMatrix[9] + colMatrix[13];
        result[2] = vec[0]*colMatrix[2] + vec[1]*colMatrix[6] + vec[2]*colMatrix[10] + colMatrix[14];

        return result;
    }

    /**
     * Matrix Vector multiplication
     * @param rawMatrix column matrix (4x4)
     * @param vec vector(x,y,z)
     * @return result
     */
    public static float[] mulRowMat4Vec3(final float[] result, final float[] rawMatrix, final float[] vec)
    {
        result[0] = vec[0]*rawMatrix[0] + vec[1]*rawMatrix[1] + vec[2]*rawMatrix[2] + rawMatrix[3];
        result[1] = vec[0]*rawMatrix[4] + vec[1]*rawMatrix[5] + vec[2]*rawMatrix[6] + rawMatrix[7];
        result[2] = vec[0]*rawMatrix[8] + vec[1]*rawMatrix[9] + vec[2]*rawMatrix[10] + rawMatrix[11];

        return result;
    }

    /**
     * Calculate the midpoint of two values
     * @param p1 first value
     * @param p2 second vale
     * @return midpoint
     */
    public static float mid(final float p1, final float p2) {
        return (p1+p2)/2.0f;
    }

    /**
     * Calculate the midpoint of two points
     * @param p1 first point vector
     * @param p2 second point vector
     * @return midpoint
     */
    public static float[] midVec3(final float[] result, final float[] p1, final float[] p2) {
        result[0] = (p1[0] + p2[0])*0.5f;
        result[1] = (p1[1] + p2[1])*0.5f;
        result[2] = (p1[2] + p2[2])*0.5f;
        return result;
    }

    /**
     * Compute the determinant of 3 vectors
     * @param a vector 1
     * @param b vector 2
     * @param c vector 3
     * @return the determinant value
     */
    public static float vec3Determinant(final float[] a, final float[] b, final float[] c) {
        return a[0]*b[1]*c[2] + a[1]*b[2]*c[0] + a[2]*b[0]*c[1] - a[0]*b[2]*c[1] - a[1]*b[0]*c[2] - a[2]*b[1]*c[0];
    }

    /**
     * Check if three vertices are colliniear
     * @param v1 vertex 1
     * @param v2 vertex 2
     * @param v3 vertex 3
     * @return true if collinear, false otherwise
     */
    public static boolean isVec3Collinear(final float[] v1, final float[] v2, final float[] v3) {
        return FloatUtil.isZero( vec3Determinant(v1, v2, v3), FloatUtil.EPSILON );
    }

    /**
     * Check if vertices in triangle circumcircle
     * @param a triangle vertex 1
     * @param b triangle vertex 2
     * @param c triangle vertex 3
     * @param d vertex in question
     * @return true if the vertex d is inside the circle defined by the
     * vertices a, b, c. from paper by Guibas and Stolfi (1985).
     */
    public static boolean isInCircle(final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c, final Vert2fImmutable d) {
        final float[] A = a.getCoord();
        final float[] B = b.getCoord();
        final float[] C = c.getCoord();
        final float[] D = d.getCoord();
        return (A[0] * A[0] + A[1] * A[1]) * triArea(B, C, D) -
               (B[0] * B[0] + B[1] * B[1]) * triArea(A, C, D) +
               (C[0] * C[0] + C[1] * C[1]) * triArea(A, B, D) -
               (D[0] * D[0] + D[1] * D[1]) * triArea(A, B, C) > 0;
    }

    /**
     * Computes oriented area of a triangle
     * @param a first vertex
     * @param b second vertex
     * @param c third vertex
     * @return compute twice the area of the oriented triangle (a,b,c), the area
     * is positive if the triangle is oriented counterclockwise.
     */
    public static float triArea(final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c){
        final float[] A = a.getCoord();
        final float[] B = b.getCoord();
        final float[] C = c.getCoord();
        return (B[0] - A[0]) * (C[1] - A[1]) - (B[1] - A[1]) * (C[0] - A[0]);
    }

    /**
     * Computes oriented area of a triangle
     * @param A first vertex
     * @param B second vertex
     * @param C third vertex
     * @return compute twice the area of the oriented triangle (a,b,c), the area
     * is positive if the triangle is oriented counterclockwise.
     */
    public static float triArea(final float[] A, final float[] B, final float[] C){
        return (B[0] - A[0]) * (C[1] - A[1]) - (B[1] - A[1])*(C[0] - A[0]);
    }

    /**
     * Check if a vertex is in triangle using
     * barycentric coordinates computation.
     * @param a first triangle vertex
     * @param b second triangle vertex
     * @param c third triangle vertex
     * @param p the vertex in question
     * @return true if p is in triangle (a, b, c), false otherwise.
     */
    public static boolean isVec3InTriangle(final float[] a, final float[]  b, final float[]  c,
                                           final float[] p,
                                           final float[] ac, final float[] ab, final float[] ap){
        // Compute vectors
        subVec3(ac, c, a); //v0
        subVec3(ab, b, a); //v1
        subVec3(ap, p, a); //v2

        // Compute dot products
        final float dot00 = vec3Dot(ac, ac);
        final float dot01 = vec3Dot(ac, ab);
        final float dot02 = vec3Dot(ac, ap);
        final float dot11 = vec3Dot(ab, ab);
        final float dot12 = vec3Dot(ab, ap);

        // Compute barycentric coordinates
        final float invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
        final float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        final float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        // Check if point is in triangle
        return (u >= 0) && (v >= 0) && (u + v < 1);
    }

    /**
     * Check if one of three vertices are in triangle using
     * barycentric coordinates computation.
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
    public static boolean isVec3InTriangle3(final float[] a, final float[]  b, final float[]  c,
                                            final float[] p1, final float[] p2, final float[] p3,
                                            final float[] tmpAC, final float[] tmpAB, final float[] tmpAP){
        // Compute vectors
        subVec3(tmpAC, c, a); //v0
        subVec3(tmpAB, b, a); //v1

        // Compute dot products
        final float dotAC_AC = vec3Dot(tmpAC, tmpAC);
        final float dotAC_AB = vec3Dot(tmpAC, tmpAB);
        final float dotAB_AB = vec3Dot(tmpAB, tmpAB);

        // Compute barycentric coordinates
        final float invDenom = 1 / (dotAC_AC * dotAB_AB - dotAC_AB * dotAC_AB);
        {
            subVec3(tmpAP, p1, a); //v2
            final float dotAC_AP1 = vec3Dot(tmpAC, tmpAP);
            final float dotAB_AP1 = vec3Dot(tmpAB, tmpAP);
            final float u1 = (dotAB_AB * dotAC_AP1 - dotAC_AB * dotAB_AP1) * invDenom;
            final float v1 = (dotAC_AC * dotAB_AP1 - dotAC_AB * dotAC_AP1) * invDenom;

            // Check if point is in triangle
            if ( (u1 >= 0) && (v1 >= 0) && (u1 + v1 < 1) ) {
                return true;
            }
        }

        {
            subVec3(tmpAP, p1, a); //v2
            final float dotAC_AP2 = vec3Dot(tmpAC, tmpAP);
            final float dotAB_AP2 = vec3Dot(tmpAB, tmpAP);
            final float u = (dotAB_AB * dotAC_AP2 - dotAC_AB * dotAB_AP2) * invDenom;
            final float v = (dotAC_AC * dotAB_AP2 - dotAC_AB * dotAC_AP2) * invDenom;

            // Check if point is in triangle
            if ( (u >= 0) && (v >= 0) && (u + v < 1) ) {
                return true;
            }
        }

        {
            subVec3(tmpAP, p2, a); //v2
            final float dotAC_AP3 = vec3Dot(tmpAC, tmpAP);
            final float dotAB_AP3 = vec3Dot(tmpAB, tmpAP);
            final float u = (dotAB_AB * dotAC_AP3 - dotAC_AB * dotAB_AP3) * invDenom;
            final float v = (dotAC_AC * dotAB_AP3 - dotAC_AB * dotAC_AP3) * invDenom;

            // Check if point is in triangle
            if ( (u >= 0) && (v >= 0) && (u + v < 1) ) {
                return true;
            }
        }

        return false;
    }

    /** Check if points are in ccw order
     * @param a first vertex
     * @param b second vertex
     * @param c third vertex
     * @return true if the points a,b,c are in a ccw order
     */
    public static boolean ccw(final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c){
        return triArea(a,b,c) > 0;
    }

    /** Compute the winding of given points
     * @param a first vertex
     * @param b second vertex
     * @param c third vertex
     * @return Winding
     */
    public static Winding getWinding(final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c) {
        return triArea(a,b,c) > 0 ? Winding.CCW : Winding.CW ;
    }

    /** Computes the area of a list of vertices to check if ccw
     * @param vertices
     * @return positive area if ccw else negative area value
     */
    public static float area(final ArrayList<? extends Vert2fImmutable> vertices) {
        final int n = vertices.size();
        float area = 0.0f;
        for (int p = n - 1, q = 0; q < n; p = q++)
        {
            final float[] pCoord = vertices.get(p).getCoord();
            final float[] qCoord = vertices.get(q).getCoord();
            area += pCoord[0] * qCoord[1] - qCoord[0] * pCoord[1];
        }
        return area;
    }

    /** Compute the  general winding of the vertices
     * @param vertices array of Vertices
     * @return CCW or CW {@link Winding}
     */
    public static Winding getWinding(final ArrayList<? extends Vert2fImmutable> vertices) {
        return area(vertices) >= 0 ? Winding.CCW : Winding.CW ;
    }


    /** Compute intersection between two segments
     * @param a vertex 1 of first segment
     * @param b vertex 2 of first segment
     * @param c vertex 1 of second segment
     * @param d vertex 2 of second segment
     * @return the intersection coordinates if the segments intersect, otherwise returns null
     */
    public static float[] seg2SegIntersection(final float[] result, final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c, final Vert2fImmutable d) {
        final float determinant = (a.getX()-b.getX())*(c.getY()-d.getY()) - (a.getY()-b.getY())*(c.getX()-d.getX());

        if (determinant == 0)
            return null;

        final float alpha = (a.getX()*b.getY()-a.getY()*b.getX());
        final float beta = (c.getX()*d.getY()-c.getY()*d.getY());
        final float xi = ((c.getX()-d.getX())*alpha-(a.getX()-b.getX())*beta)/determinant;
        final float yi = ((c.getY()-d.getY())*alpha-(a.getY()-b.getY())*beta)/determinant;

        final float gamma = (xi - a.getX())/(b.getX() - a.getX());
        final float gamma1 = (xi - c.getX())/(d.getX() - c.getX());
        if(gamma <= 0 || gamma >= 1) return null;
        if(gamma1 <= 0 || gamma1 >= 1) return null;

        result[0] = xi;
        result[1] = yi;
        result[2] = 0;
        return result;
    }

    /**
     * Compute intersection between two segments
     * @param a vertex 1 of first segment
     * @param b vertex 2 of first segment
     * @param c vertex 1 of second segment
     * @param d vertex 2 of second segment
     * @return true if the segments intersect, otherwise returns false
     */
    public static boolean testSeg2SegIntersection(final Vert2fImmutable a, final Vert2fImmutable b,
                                                  final Vert2fImmutable c, final Vert2fImmutable d) {
        final float[] A = a.getCoord();
        final float[] B = b.getCoord();
        final float[] C = c.getCoord();
        final float[] D = d.getCoord();

        final float determinant = (A[0]-B[0])*(C[1]-D[1]) - (A[1]-B[1])*(C[0]-D[0]);

        if (determinant == 0) {
            return false;
        }

        final float alpha = (A[0]*B[1]-A[1]*B[0]);
        final float beta = (C[0]*D[1]-C[1]*D[1]);
        final float xi = ((C[0]-D[0])*alpha-(A[0]-B[0])*beta)/determinant;

        final float gamma = (xi - A[0])/(B[0] - A[0]);
        final float gamma1 = (xi - C[0])/(D[0] - C[0]);
        if(gamma <= 0 || gamma >= 1 || gamma1 <= 0 || gamma1 >= 1) {
            return false;
        }

        return true;
    }

    /** Compute intersection between two lines
     * @param a vertex 1 of first line
     * @param b vertex 2 of first line
     * @param c vertex 1 of second line
     * @param d vertex 2 of second line
     * @return the intersection coordinates if the lines intersect, otherwise
     * returns null
     */
    public static float[] line2lineIntersection(final float[] result,
                                                final Vert2fImmutable a, final Vert2fImmutable b,
                                                final Vert2fImmutable c, final Vert2fImmutable d) {
        final float determinant = (a.getX()-b.getX())*(c.getY()-d.getY()) - (a.getY()-b.getY())*(c.getX()-d.getX());

        if (determinant == 0)
            return null;

        final float alpha = (a.getX()*b.getY()-a.getY()*b.getX());
        final float beta = (c.getX()*d.getY()-c.getY()*d.getY());
        final float xi = ((c.getX()-d.getX())*alpha-(a.getX()-b.getX())*beta)/determinant;
        final float yi = ((c.getY()-d.getY())*alpha-(a.getY()-b.getY())*beta)/determinant;

        result[0] = xi;
        result[1] = yi;
        result[2] = 0;
        return result;
    }

    /** Check if a segment intersects with a triangle
     * @param a vertex 1 of the triangle
     * @param b vertex 2 of the triangle
     * @param c vertex 3 of the triangle
     * @param d vertex 1 of first segment
     * @param e vertex 2 of first segment
     * @return true if the segment intersects at least one segment of the triangle, false otherwise
     */
    public static boolean testTri2SegIntersection(final Vert2fImmutable a, final Vert2fImmutable b, final Vert2fImmutable c,
                                                  final Vert2fImmutable d, final Vert2fImmutable e){
        return testSeg2SegIntersection(a, b, d, e) ||
               testSeg2SegIntersection(b, c, d, e) ||
               testSeg2SegIntersection(a, c, d, e) ;
    }
}
