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

    public enum Winding {
        CW(-1), CCW(1);

        public final int dir;

        Winding(int dir) {
            this.dir = dir;
        }
    }

    public static final int COLLINEAR = 0;

    /** compute the dot product of two points
     * @param vec1 vector 1
     * @param vec2 vector 2
     * @return the dot product as float
     */
    public static float dot(float[] vec1, float[] vec2)
    {
        return (vec1[0]*vec2[0] + vec1[1]*vec2[1] + vec1[2]*vec2[2]);
    }
    /** Normalize a vector
     * @param vector input vector
     * @return normalized vector
     */
    public static float[] normalize(float[] vector)
    {
        final float[] newVector = new float[3];

        final float d = FloatUtil.sqrt(vector[0]*vector[0] + vector[1]*vector[1] + vector[2]*vector[2]);
        if(d> 0.0f)
        {
            newVector[0] = vector[0]/d;
            newVector[1] = vector[1]/d;
            newVector[2] = vector[2]/d;
        }
        return newVector;
    }

    /** Scales a vector by param creating a new float[] for the result!
     * @param vector input vector
     * @param scale constant to scale by
     * @return new scaled vector
     * @deprecated Use {@link #scale(float[], float[], float)}
     */
    public static float[] scale(float[] vector, float scale)
    {
        final float[] newVector = new float[3];

        newVector[0] = vector[0] * scale;
        newVector[1] = vector[1] * scale;
        newVector[2] = vector[2] * scale;
        return newVector;
    }

    /** Scales a vector by param using given result float[]
     * @param result vector for the result
     * @param vector input vector
     * @param scale single scale constant for all vector components
     */
    public static float[] scale(float[] result, float[] vector, float scale)
    {
        result[0] = vector[0] * scale;
        result[1] = vector[1] * scale;
        result[2] = vector[2] * scale;
        return result;
    }

    /** Scales a vector by param using given result float[]
     * @param result vector for the result
     * @param vector input vector
     * @param scale 3 component scale constant for each vector component
     * @return given result vector
     */
    public static float[] scale(float[] result, float[] vector, float[] scale)
    {
        result[0] = vector[0] * scale[0];
        result[1] = vector[1] * scale[1];
        result[2] = vector[2] * scale[2];
        return result;
    }

    /** Adds to vectors
     * @param v1 vector 1
     * @param v2 vector 2
     * @return v1 + v2
     */
    public static float[] vectorAdd(float[] v1, float[] v2)
    {
        final float[] newVector = new float[3];

        newVector[0] = v1[0] + v2[0];
        newVector[1] = v1[1] + v2[1];
        newVector[2] = v1[2] + v2[2];
        return newVector;
    }

    /** cross product vec1 x vec2
     * @param vec1 vector 1
     * @param vec2 vecttor 2
     * @return the resulting vector
     */
    public static float[] cross(float[] vec1, float[] vec2)
    {
        final float[] out = new float[3];

        out[0] = vec2[2]*vec1[1] - vec2[1]*vec1[2];
        out[1] = vec2[0]*vec1[2] - vec2[2]*vec1[0];
        out[2] = vec2[1]*vec1[0] - vec2[0]*vec1[1];

        return out;
    }

    /** Column Matrix Vector multiplication
     * @param colMatrix column matrix (4x4)
     * @param vec vector(x,y,z)
     * @return result new float[3]
     */
    public static float[] colMatrixVectorMult(float[] colMatrix, float[] vec)
    {
        final float[] out = new float[3];

        out[0] = vec[0]*colMatrix[0] + vec[1]*colMatrix[4] + vec[2]*colMatrix[8] + colMatrix[12];
        out[1] = vec[0]*colMatrix[1] + vec[1]*colMatrix[5] + vec[2]*colMatrix[9] + colMatrix[13];
        out[2] = vec[0]*colMatrix[2] + vec[1]*colMatrix[6] + vec[2]*colMatrix[10] + colMatrix[14];

        return out;
    }

    /** Matrix Vector multiplication
     * @param rawMatrix column matrix (4x4)
     * @param vec vector(x,y,z)
     * @return result new float[3]
     */
    public static float[] rowMatrixVectorMult(float[] rawMatrix, float[] vec)
    {
        final float[] out = new float[3];

        out[0] = vec[0]*rawMatrix[0] + vec[1]*rawMatrix[1] + vec[2]*rawMatrix[2] + rawMatrix[3];
        out[1] = vec[0]*rawMatrix[4] + vec[1]*rawMatrix[5] + vec[2]*rawMatrix[6] + rawMatrix[7];
        out[2] = vec[0]*rawMatrix[8] + vec[1]*rawMatrix[9] + vec[2]*rawMatrix[10] + rawMatrix[11];

        return out;
    }

    /** Calculate the midpoint of two values
     * @param p1 first value
     * @param p2 second vale
     * @return midpoint
     */
    public static float mid(float p1, float p2)
    {
        return (p1+p2)/2.0f;
    }

    /** Calculate the midpoint of two points
     * @param p1 first point
     * @param p2 second point
     * @return midpoint
     */
    public static float[] mid(float[] p1, float[] p2)
    {
        final float[] midPoint = new float[3];
        midPoint[0] = (p1[0] + p2[0])*0.5f;
        midPoint[1] = (p1[1] + p2[1])*0.5f;
        midPoint[2] = (p1[2] + p2[2])*0.5f;

        return midPoint;
    }

    /** Compute the norm of a vector
     * @param vec vector
     * @return vorm
     */
    public static float norm(float[] vec)
    {
        return FloatUtil.sqrt(vec[0]*vec[0] + vec[1]*vec[1] + vec[2]*vec[2]);
    }

    /** Compute distance between 2 points
     * @param p0 a ref point on the line
     * @param vec vector representing the direction of the line
     * @param point the point to compute the relative distance of
     * @return distance float
     */
    public static float computeLength(float[] p0, float[] point)
    {
        final float w0 = point[0]-p0[0];
        final float w1 = point[1]-p0[1];
        final float w2 = point[2]-p0[2];

        return FloatUtil.sqrt(w0*w0 + w1*w1 + w2*w2);
    }

    /**Check equality of 2 vec3 vectors
     * @param v1 vertex 1
     * @param v2 vertex 2
     * @return
     */
    public static boolean checkEquality(float[] v1, float[] v2)
    {
        return Float.compare(v1[0], v2[0]) == 0 &&
               Float.compare(v1[1], v2[1]) == 0 &&
               Float.compare(v1[2], v2[2]) == 0 ;
    }

    /**Check equality of 2 vec2 vectors
     * @param v1 vertex 1
     * @param v2 vertex 2
     * @return
     */
    public static boolean checkEqualityVec2(float[] v1, float[] v2)
    {
        return Float.compare(v1[0], v2[0]) == 0 &&
               Float.compare(v1[1], v2[1]) == 0 ;
    }

    /** Compute the determinant of 3 vectors
     * @param a vector 1
     * @param b vector 2
     * @param c vector 3
     * @return the determinant value
     */
    public static float computeDeterminant(float[] a, float[] b, float[] c)
    {
        return a[0]*b[1]*c[2] + a[1]*b[2]*c[0] + a[2]*b[0]*c[1] - a[0]*b[2]*c[1] - a[1]*b[0]*c[2] - a[2]*b[1]*c[0];
    }

    /** Check if three vertices are colliniear
     * @param v1 vertex 1
     * @param v2 vertex 2
     * @param v3 vertex 3
     * @return true if collinear, false otherwise
     */
    public static boolean checkCollinear(float[] v1, float[] v2, float[] v3)
    {
        return (computeDeterminant(v1, v2, v3) == VectorUtil.COLLINEAR);
    }

    /** Compute Vector
     * @param vector storage for resulting Vector V1V2
     * @param v1 vertex 1
     * @param v2 vertex2 2
     */
    public static void computeVector(float[] vector, float[] v1, float[] v2) {
        vector[0] = v2[0] - v1[0];
        vector[1] = v2[1] - v1[1];
        vector[2] = v2[2] - v1[2];
    }

    /** Check if vertices in triangle circumcircle
     * @param a triangle vertex 1
     * @param b triangle vertex 2
     * @param c triangle vertex 3
     * @param d vertex in question
     * @return true if the vertex d is inside the circle defined by the
     * vertices a, b, c. from paper by Guibas and Stolfi (1985).
     */
    public static boolean inCircle(Vert2fImmutable a, Vert2fImmutable b, Vert2fImmutable c, Vert2fImmutable d) {
        final float[] A = a.getCoord();
        final float[] B = b.getCoord();
        final float[] C = c.getCoord();
        final float[] D = d.getCoord();
        return (A[0] * A[0] + A[1] * A[1]) * triArea(B, C, D) -
               (B[0] * B[0] + B[1] * B[1]) * triArea(A, C, D) +
               (C[0] * C[0] + C[1] * C[1]) * triArea(A, B, D) -
               (D[0] * D[0] + D[1] * D[1]) * triArea(A, B, C) > 0;
    }

    /** Computes oriented area of a triangle
     * @param a first vertex
     * @param b second vertex
     * @param c third vertex
     * @return compute twice the area of the oriented triangle (a,b,c), the area
     * is positive if the triangle is oriented counterclockwise.
     */
    public static float triArea(Vert2fImmutable a, Vert2fImmutable b, Vert2fImmutable c){
        final float[] A = a.getCoord();
        final float[] B = b.getCoord();
        final float[] C = c.getCoord();
        return (B[0] - A[0]) * (C[1] - A[1]) - (B[1] - A[1]) * (C[0] - A[0]);
    }

    /** Computes oriented area of a triangle
     * @param A first vertex
     * @param B second vertex
     * @param C third vertex
     * @return compute twice the area of the oriented triangle (a,b,c), the area
     * is positive if the triangle is oriented counterclockwise.
     */
    public static float triArea(float[] A, float[] B, float[] C){
        return (B[0] - A[0]) * (C[1] - A[1]) - (B[1] - A[1])*(C[0] - A[0]);
    }

    /** Check if a vertex is in triangle using
     * barycentric coordinates computation.
     * @param a first triangle vertex
     * @param b second triangle vertex
     * @param c third triangle vertex
     * @param p the vertex in question
     * @return true if p is in triangle (a, b, c), false otherwise.
     */
    public static boolean vertexInTriangle(float[] a, float[]  b, float[]  c,
                                           float[] p,
                                           float[] ac, float[] ab, float[] ap){
        // Compute vectors
        computeVector(ac, a, c); //v0
        computeVector(ab, a, b); //v1
        computeVector(ap, a, p); //v2

        // Compute dot products
        final float dot00 = dot(ac, ac);
        final float dot01 = dot(ac, ab);
        final float dot02 = dot(ac, ap);
        final float dot11 = dot(ab, ab);
        final float dot12 = dot(ab, ap);

        // Compute barycentric coordinates
        final float invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
        final float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        final float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        // Check if point is in triangle
        return (u >= 0) && (v >= 0) && (u + v < 1);
    }

    /** Check if one of three vertices are in triangle using
     * barycentric coordinates computation.
     * @param a first triangle vertex
     * @param b second triangle vertex
     * @param c third triangle vertex
     * @param p1 the vertex in question
     * @param p2 the vertex in question
     * @param p3 the vertex in question
     * @return true if p1 or p2 or p3 is in triangle (a, b, c), false otherwise.
     */
    public static boolean vertexInTriangle3(float[] a, float[]  b, float[]  c,
                                            float[] p1, float[] p2, float[] p3,
                                            float[] ac, float[] ab, float[] ap){
        // Compute vectors
        computeVector(ac, a, c); //v0
        computeVector(ab, a, b); //v1

        // Compute dot products
        final float dotAC_AC = dot(ac, ac);
        final float dotAC_AB = dot(ac, ab);
        final float dotAB_AB = dot(ab, ab);

        // Compute barycentric coordinates
        final float invDenom = 1 / (dotAC_AC * dotAB_AB - dotAC_AB * dotAC_AB);
        {
            computeVector(ap, a, p1); //v2
            final float dotAC_AP1 = dot(ac, ap);
            final float dotAB_AP1 = dot(ab, ap);
            final float u1 = (dotAB_AB * dotAC_AP1 - dotAC_AB * dotAB_AP1) * invDenom;
            final float v1 = (dotAC_AC * dotAB_AP1 - dotAC_AB * dotAC_AP1) * invDenom;

            // Check if point is in triangle
            if ( (u1 >= 0) && (v1 >= 0) && (u1 + v1 < 1) ) {
                return true;
            }
        }

        {
            computeVector(ap, a, p2); //v2
            final float dotAC_AP2 = dot(ac, ap);
            final float dotAB_AP2 = dot(ab, ap);
            final float u = (dotAB_AB * dotAC_AP2 - dotAC_AB * dotAB_AP2) * invDenom;
            final float v = (dotAC_AC * dotAB_AP2 - dotAC_AB * dotAC_AP2) * invDenom;

            // Check if point is in triangle
            if ( (u >= 0) && (v >= 0) && (u + v < 1) ) {
                return true;
            }
        }

        {
            computeVector(ap, a, p3); //v2
            final float dotAC_AP3 = dot(ac, ap);
            final float dotAB_AP3 = dot(ab, ap);
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
    public static boolean ccw(Vert2fImmutable a, Vert2fImmutable b, Vert2fImmutable c){
        return triArea(a,b,c) > 0;
    }

    /** Compute the winding of given points
     * @param a first vertex
     * @param b second vertex
     * @param c third vertex
     * @return Winding
     */
    public static Winding getWinding(Vert2fImmutable a, Vert2fImmutable b, Vert2fImmutable c) {
        return triArea(a,b,c) > 0 ? Winding.CCW : Winding.CW ;
    }

    /** Computes the area of a list of vertices to check if ccw
     * @param vertices
     * @return positive area if ccw else negative area value
     */
    public static float area(ArrayList<? extends Vert2fImmutable> vertices) {
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
    public static Winding getWinding(ArrayList<? extends Vert2fImmutable> vertices) {
        return area(vertices) >= 0 ? Winding.CCW : Winding.CW ;
    }


    /** Compute intersection between two segments
     * @param a vertex 1 of first segment
     * @param b vertex 2 of first segment
     * @param c vertex 1 of second segment
     * @param d vertex 2 of second segment
     * @return the intersection coordinates if the segments intersect, otherwise
     * returns null
     */
    public static float[] seg2SegIntersection(Vert2fImmutable a, Vert2fImmutable b, Vert2fImmutable c, Vert2fImmutable d) {
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

        return new float[]{xi,yi,0};
    }

    /** Compute intersection between two segments
     * @param a vertex 1 of first segment
     * @param b vertex 2 of first segment
     * @param c vertex 1 of second segment
     * @param d vertex 2 of second segment
     * @return true if the segments intersect, otherwise returns false
     */
    public static boolean testSeg2SegIntersection(Vert2fImmutable a, Vert2fImmutable b, Vert2fImmutable c, Vert2fImmutable d) {
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
    public static float[] line2lineIntersection(Vert2fImmutable a, Vert2fImmutable b, Vert2fImmutable c, Vert2fImmutable d) {
        final float determinant = (a.getX()-b.getX())*(c.getY()-d.getY()) - (a.getY()-b.getY())*(c.getX()-d.getX());

        if (determinant == 0)
            return null;

        final float alpha = (a.getX()*b.getY()-a.getY()*b.getX());
        final float beta = (c.getX()*d.getY()-c.getY()*d.getY());
        final float xi = ((c.getX()-d.getX())*alpha-(a.getX()-b.getX())*beta)/determinant;
        final float yi = ((c.getY()-d.getY())*alpha-(a.getY()-b.getY())*beta)/determinant;

        return new float[]{xi,yi,0};
    }

    /** Check if a segment intersects with a triangle
     * @param a vertex 1 of the triangle
     * @param b vertex 2 of the triangle
     * @param c vertex 3 of the triangle
     * @param d vertex 1 of first segment
     * @param e vertex 2 of first segment
     * @return true if the segment intersects at least one segment of the triangle, false otherwise
     */
    public static boolean testTri2SegIntersection(Vert2fImmutable a, Vert2fImmutable b, Vert2fImmutable c, Vert2fImmutable d, Vert2fImmutable e){
        return testSeg2SegIntersection(a, b, d, e) ||
               testSeg2SegIntersection(b, c, d, e) ||
               testSeg2SegIntersection(a, c, d, e) ;
    }
}
