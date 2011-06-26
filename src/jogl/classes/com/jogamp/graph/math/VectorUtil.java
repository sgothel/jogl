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
package com.jogamp.graph.math;

import java.util.ArrayList;

import jogamp.graph.math.MathFloat;

import com.jogamp.graph.geom.Vertex;

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
        float[] newVector = new float[3];

        float d = MathFloat.sqrt(vector[0]*vector[0] + vector[1]*vector[1] + vector[2]*vector[2]);
        if(d> 0.0f)
        {
            newVector[0] = vector[0]/d;
            newVector[1] = vector[1]/d;
            newVector[2] = vector[2]/d;
        }
        return newVector;
    }

    /** Scales a vector by param
     * @param vector input vector
     * @param scale constant to scale by
     * @return scaled vector
     */
    public static float[] scale(float[] vector, float scale)
    {
        float[] newVector = new float[3];

        newVector[0] = vector[0]*scale;
        newVector[1] = vector[1]*scale;
        newVector[2] = vector[2]*scale;
        return newVector;
    }

    /** Adds to vectors
     * @param v1 vector 1
     * @param v2 vector 2
     * @return v1 + v2
     */
    public static float[] vectorAdd(float[] v1, float[] v2)
    {
        float[] newVector = new float[3];

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
        float[] out = new float[3];

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
        float[] out = new float[3];

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
        float[] out = new float[3];

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
        float[] midPoint = new float[3];
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
        return MathFloat.sqrt(vec[0]*vec[0] + vec[1]*vec[1] + vec[2]*vec[2]);
    }
    /** Compute distance between 2 points
     * @param p0 a ref point on the line
     * @param vec vector representing the direction of the line
     * @param point the point to compute the relative distance of
     * @return distance float
     */
    public static float computeLength(float[] p0, float[] point)
    {
        float[] w = new float[]{point[0]-p0[0],point[1]-p0[1],point[2]-p0[2]};

        float distance = MathFloat.sqrt(w[0]*w[0] + w[1]*w[1] + w[2]*w[2]);

        return distance;
    }

    /**Check equality of 2 vec3 vectors
     * @param v1 vertex 1
     * @param v2 vertex 2
     * @return
     */
    public static boolean checkEquality(float[] v1, float[] v2)
    {
        if(Float.compare(v1[0], v2[0]) == 0 &&
                Float.compare(v1[1], v2[1]) == 0 &&
                Float.compare(v1[2], v2[2]) == 0 )
            return true;
        return false;
    }

    /**Check equality of 2 vec2 vectors
     * @param v1 vertex 1
     * @param v2 vertex 2
     * @return
     */
    public static boolean checkEqualityVec2(float[] v1, float[] v2)
    {
        if(Float.compare(v1[0], v2[0]) == 0 && 
                Float.compare(v1[1], v2[1]) == 0)
            return true;
        return false;
    }

    /** Compute the determinant of 3 vectors
     * @param a vector 1
     * @param b vector 2
     * @param c vector 3
     * @return the determinant value
     */
    public static float computeDeterminant(float[] a, float[] b, float[] c)
    {
        float area = a[0]*b[1]*c[2] + a[1]*b[2]*c[0] + a[2]*b[0]*c[1] - a[0]*b[2]*c[1] - a[1]*b[0]*c[2] - a[2]*b[1]*c[0];
        return area;
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
     * @param v1 vertex 1
     * @param v2 vertex2 2
     * @return Vector V1V2
     */
    public static float[] computeVector(float[] v1, float[] v2)
    {
        float[] vector = new float[3];
        vector[0] = v2[0] - v1[0];
        vector[1] = v2[1] - v1[1];
        vector[2] = v2[2] - v1[2];
        return vector;
    }

    /** Check if vertices in triangle circumcircle
     * @param a triangle vertex 1
     * @param b triangle vertex 2
     * @param c triangle vertex 3
     * @param d vertex in question
     * @return true if the vertex d is inside the circle defined by the 
     * vertices a, b, c. from paper by Guibas and Stolfi (1985).
     */
    public static boolean inCircle(Vertex a, Vertex b, Vertex c, Vertex d){
        return (a.getX() * a.getX() + a.getY() * a.getY()) * triArea(b, c, d) -
        (b.getX() * b.getX() + b.getY() * b.getY()) * triArea(a, c, d) +
        (c.getX() * c.getX() + c.getY() * c.getY()) * triArea(a, b, d) -
        (d.getX() * d.getX() + d.getY() * d.getY()) * triArea(a, b, c) > 0;
    }

    /** Computes oriented area of a triangle
     * @param a first vertex
     * @param b second vertex
     * @param c third vertex
     * @return compute twice the area of the oriented triangle (a,b,c), the area
     * is positive if the triangle is oriented counterclockwise.
     */
    public static float triArea(Vertex a, Vertex b, Vertex c){
        return (b.getX() - a.getX()) * (c.getY() - a.getY()) - (b.getY() - a.getY())*(c.getX() - a.getX());
    }

    /** Check if a vertex is in triangle using 
     * barycentric coordinates computation. 
     * @param a first triangle vertex
     * @param b second triangle vertex
     * @param c third triangle vertex
     * @param p the vertex in question
     * @return true if p is in triangle (a, b, c), false otherwise.
     */
    public static boolean vertexInTriangle(float[] a, float[]  b, float[]  c, float[]  p){
        // Compute vectors        
        float[] ac = computeVector(a, c); //v0
        float[] ab = computeVector(a, b); //v1
        float[] ap = computeVector(a, p); //v2

        // Compute dot products
        float dot00 = dot(ac, ac);
        float dot01 = dot(ac, ab);
        float dot02 = dot(ac, ap);
        float dot11 = dot(ab, ab);
        float dot12 = dot(ab, ap);

        // Compute barycentric coordinates
        float invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
        float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        // Check if point is in triangle
        return (u >= 0) && (v >= 0) && (u + v < 1);
    }

    /** Check if points are in ccw order
     * @param a first vertex
     * @param b second vertex
     * @param c third vertex
     * @return true if the points a,b,c are in a ccw order
     */
    public static boolean ccw(Vertex a, Vertex b, Vertex c){
        return triArea(a,b,c) > 0;
    }

    /** Compute the winding of given points
     * @param a first vertex
     * @param b second vertex
     * @param c third vertex
     * @return Winding
     */
    public static Winding getWinding(Vertex a, Vertex b, Vertex c) {
        return triArea(a,b,c) > 0 ? Winding.CCW : Winding.CW ;
    }

    /** Computes the area of a list of vertices to check if ccw
     * @param vertices
     * @return positive area if ccw else negative area value
     */
    public static float area(ArrayList<Vertex> vertices) {
        int n = vertices.size();
        float area = 0.0f;
        for (int p = n - 1, q = 0; q < n; p = q++)
        {
            float[] pCoord = vertices.get(p).getCoord();
            float[] qCoord = vertices.get(q).getCoord();
            area += pCoord[0] * qCoord[1] - qCoord[0] * pCoord[1];
        }
        return area;
    }

    /** Compute the  general winding of the vertices
     * @param vertices array of Vertices
     * @return CCW or CW {@link Winding}
     */
    public static Winding getWinding(ArrayList<Vertex> vertices) {
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
    public static float[] seg2SegIntersection(Vertex a, Vertex b, Vertex c, Vertex d) {
        float determinant = (a.getX()-b.getX())*(c.getY()-d.getY()) - (a.getY()-b.getY())*(c.getX()-d.getX());

        if (determinant == 0) 
            return null;

        float alpha = (a.getX()*b.getY()-a.getY()*b.getX());
        float beta = (c.getX()*d.getY()-c.getY()*d.getY());
        float xi = ((c.getX()-d.getX())*alpha-(a.getX()-b.getX())*beta)/determinant;
        float yi = ((c.getY()-d.getY())*alpha-(a.getY()-b.getY())*beta)/determinant;

        float gamma = (xi - a.getX())/(b.getX() - a.getX());
        float gamma1 = (xi - c.getX())/(d.getX() - c.getX());
        if(gamma <= 0 || gamma >= 1) return null;
        if(gamma1 <= 0 || gamma1 >= 1) return null;

        return new float[]{xi,yi,0};
    }

    /** Compute intersection between two lines
     * @param a vertex 1 of first line
     * @param b vertex 2 of first line
     * @param c vertex 1 of second line
     * @param d vertex 2 of second line
     * @return the intersection coordinates if the lines intersect, otherwise 
     * returns null 
     */
    public static float[] line2lineIntersection(Vertex a, Vertex b, Vertex c, Vertex d) {
        float determinant = (a.getX()-b.getX())*(c.getY()-d.getY()) - (a.getY()-b.getY())*(c.getX()-d.getX());

        if (determinant == 0) 
            return null;

        float alpha = (a.getX()*b.getY()-a.getY()*b.getX());
        float beta = (c.getX()*d.getY()-c.getY()*d.getY());
        float xi = ((c.getX()-d.getX())*alpha-(a.getX()-b.getX())*beta)/determinant;
        float yi = ((c.getY()-d.getY())*alpha-(a.getY()-b.getY())*beta)/determinant;

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
    public static boolean tri2SegIntersection(Vertex a, Vertex b, Vertex c, Vertex d, Vertex e){
        if(seg2SegIntersection(a, b, d, e) != null)
            return true;
        if(seg2SegIntersection(b, c, d, e) != null)
            return true;
        if(seg2SegIntersection(a, c, d, e) != null)
            return true;

        return false;
    }
}
