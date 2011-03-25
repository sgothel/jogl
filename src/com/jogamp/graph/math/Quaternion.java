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

import jogamp.graph.math.MathFloat;

public class Quaternion {
	protected float x,y,z,w;

	public Quaternion(){

	}
	
	public Quaternion(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	/** Constructor to create a rotation based quaternion from two vectors
	 * @param vector1
	 * @param vector2
	 */
	public Quaternion(float[] vector1, float[] vector2) 
	{
		float theta = (float)MathFloat.acos(dot(vector1, vector2));
		float[] cross = cross(vector1,vector2);
		cross = normalizeVec(cross);

		this.x = (float)MathFloat.sin(theta/2)*cross[0];
		this.y = (float)MathFloat.sin(theta/2)*cross[1];
		this.z = (float)MathFloat.sin(theta/2)*cross[2];
		this.w = (float)MathFloat.cos(theta/2);
		this.normalize();
	}
	
	/** Transform the rotational quaternion to axis based rotation angles
	 * @return new float[4] with ,theta,Rx,Ry,Rz
	 */
	public float[] toAxis()
	{
		float[] vec = new float[4];
		float scale = (float)MathFloat.sqrt(x * x + y * y + z * z);
		vec[0] =(float) MathFloat.acos(w) * 2.0f;
		vec[1] = x / scale;
		vec[2] = y / scale;
		vec[3] = z / scale;
		return vec;
	}
	
	/** Normalize a vector
	 * @param vector input vector
	 * @return normalized vector
	 */
	private float[] normalizeVec(float[] vector)
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
	/** compute the dot product of two points
	 * @param vec1 vector 1
	 * @param vec2 vector 2
	 * @return the dot product as float
	 */
	private float dot(float[] vec1, float[] vec2)
	{
		return (vec1[0]*vec2[0] + vec1[1]*vec2[1] + vec1[2]*vec2[2]);
	}
	/** cross product vec1 x vec2
	 * @param vec1 vector 1
	 * @param vec2 vecttor 2
	 * @return the resulting vector
	 */
	private float[] cross(float[] vec1, float[] vec2)
	{
		float[] out = new float[3];

		out[0] = vec2[2]*vec1[1] - vec2[1]*vec1[2];
		out[1] = vec2[0]*vec1[2] - vec2[2]*vec1[0];
		out[2] = vec2[1]*vec1[0] - vec2[0]*vec1[1];

		return out;
	}
	public float getW() {
		return w;
	}
	public void setW(float w) {
		this.w = w;
	}
	public float getX() {
		return x;
	}
	public void setX(float x) {
		this.x = x;
	}
	public float getY() {
		return y;
	}
	public void setY(float y) {
		this.y = y;
	}
	public float getZ() {
		return z;
	}
	public void setZ(float z) {
		this.z = z;
	}

	/** Add a quaternion
	 * @param q quaternion
	 */
	public void add(Quaternion q)
	{
		x+=q.x;
		y+=q.y;
		z+=q.z;
	}
	
	/** Subtract a quaternion
	 * @param q quaternion
	 */
	public void subtract(Quaternion q)
	{
		x-=q.x;
		y-=q.y;
		z-=q.z;
	}
	
	/** Divide a quaternion by a constant
	 * @param n a float to divide by
	 */
	public void divide(float n)
	{
		x/=n;
		y/=n;
		z/=n;
	}
	
	/** Multiply this quaternion by 
	 * the param quaternion
	 * @param q a quaternion to multiply with
	 */
	public void mult(Quaternion q)
	{
		float w1 = w*q.w - (x*q.x + y*q.y + z*q.z);

		float x1 = w*q.z + q.w*z + y*q.z - z*q.y;
		float y1 = w*q.x + q.w*x + z*q.x - x*q.z;
		float z1 = w*q.y + q.w*y + x*q.y - y*q.x;

		w = w1;
		x = x1;
		y = y1;
		z = z1; 
	}
	
	/** Multiply a quaternion by a constant
	 * @param n a float constant
	 */
	public void mult(float n)
	{
		x*=n;
		y*=n;
		z*=n;
	}
	
	/** Normalize a quaternion required if  
	 *  to be used as a rotational quaternion
	 */
	public void normalize()
	{
		float norme = (float)MathFloat.sqrt(w*w + x*x + y*y + z*z);
		if (norme == 0.0f)
		{
			w = 1.0f; 
			x = y = z = 0.0f;
		}
		else
		{
			float recip = 1.0f/norme;

			w *= recip;
			x *= recip;
			y *= recip;
			z *= recip;
		}
	}
	
	/** Invert the quaternion If rotational, 
	 * will produce a the inverse rotation
	 */
	public void inverse()
	{
		float norm = w*w + x*x + y*y + z*z;

		float recip = 1.0f/norm;

		w *= recip;
		x = -1*x*recip;
		y = -1*y*recip;
		z = -1*z*recip;
	}
	
	/** Transform this quaternion to a
	 * 4x4 column matrix representing the rotation
	 * @return new float[16] column matrix 4x4 
	 */
	public float[] toMatrix()
	{
		float[] matrix = new float[16];
		matrix[0] = 1.0f - 2*y*y - 2*z*z;
		matrix[1] = 2*x*y + 2*w*z;
		matrix[2] = 2*x*z - 2*w*y;
		matrix[3] = 0;

		matrix[4] = 2*x*y - 2*w*z;
		matrix[5] = 1.0f - 2*x*x - 2*z*z;
		matrix[6] = 2*y*z + 2*w*x;
		matrix[7] = 0;

		matrix[8]  = 2*x*z + 2*w*y;
		matrix[9]  = 2*y*z - 2*w*x;
		matrix[10] = 1.0f - 2*x*x - 2*y*y;
		matrix[11] = 0;

		matrix[12] = 0;
		matrix[13] = 0;
		matrix[14] = 0;
		matrix[15] = 1;
		return matrix;
	}
	
	/** Set this quaternion from a Sphereical interpolation
	 *  of two param quaternion, used mostly for rotational animation
	 * @param a initial quaternion
	 * @param b target quaternion
	 * @param t float between 0 and 1 representing interp.
	 */
	public void slerp(Quaternion a,Quaternion b, float t)
	{
		float omega, cosom, sinom, sclp, sclq;
		cosom = a.x*b.x + a.y*b.y + a.z*b.z + a.w*b.w;
		if ((1.0f+cosom) > MathFloat.E) {
			if ((1.0f-cosom) > MathFloat.E) {
				omega = (float)MathFloat.acos(cosom);
				sinom = (float)MathFloat.sin(omega);
				sclp = (float)MathFloat.sin((1.0f-t)*omega) / sinom;
				sclq = (float)MathFloat.sin(t*omega) / sinom;
			}
			else {
				sclp = 1.0f - t;
				sclq = t;
			}
			x = sclp*a.x + sclq*b.x;
			y = sclp*a.y + sclq*b.y;
			z = sclp*a.z + sclq*b.z;
			w = sclp*a.w + sclq*b.w;
		}
		else {
			x =-a.y;
			y = a.x;
			z =-a.w;
			w = a.z;
			sclp = MathFloat.sin((1.0f-t) * MathFloat.PI * 0.5f);
			sclq = MathFloat.sin(t * MathFloat.PI * 0.5f);
			x = sclp*a.x + sclq*b.x;
			y = sclp*a.y + sclq*b.y;
			z = sclp*a.z + sclq*b.z;
		}
	}
	
	/** Check if this quaternion is empty, ie (0,0,0,1)
	 * @return true if empty, false otherwise
	 */
	public boolean isEmpty()
	{
		if (w==1 && x==0 && y==0 && z==0)
			return true;
		return false;
	}
	
	/** Check if this quaternion represents an identity
	 * matrix, for rotation.
	 * @return true if it is an identity rep., false otherwise
	 */
	public boolean isIdentity()
	{
		if (w==0 && x==0 && y==0 && z==0)
			return true;
		return false;
	}
	
	/** compute the quaternion from a 3x3 column matrix
	 * @param m 3x3 column matrix 
	 */
	public void setFromMatrix(float[] m) {
		float T= m[0] + m[4] + m[8] + 1;
		if (T>0){
			float S = 0.5f / (float)MathFloat.sqrt(T);
			w = 0.25f / S;
			x = ( m[5] - m[7]) * S;
			y = ( m[6] - m[2]) * S;
			z = ( m[1] - m[3] ) * S;
		}
		else{
			if ((m[0] > m[4])&(m[0] > m[8])) { 
				float S = MathFloat.sqrt( 1.0f + m[0] - m[4] - m[8] ) * 2f; // S=4*qx 
				w = (m[7] - m[5]) / S;
				x = 0.25f * S;
				y = (m[3] + m[1]) / S; 
				z = (m[6] + m[2]) / S; 
			} 
			else if (m[4] > m[8]) { 
				float S = MathFloat.sqrt( 1.0f + m[4] - m[0] - m[8] ) * 2f; // S=4*qy
				w = (m[6] - m[2]) / S;
				x = (m[3] + m[1]) / S; 
				y = 0.25f * S;
				z = (m[7] + m[5]) / S; 
			} 
			else { 
				float S = MathFloat.sqrt( 1.0f + m[8] - m[0] - m[4] ) * 2f; // S=4*qz
				w = (m[3] - m[1]) / S;
				x = (m[6] + m[2]) / S; 
				y = (m[7] + m[5]) / S; 
				z = 0.25f * S;
			} 
		}
	}
	
	/** Check if the the 3x3 matrix (param) is in fact 
	 * an affine rotational matrix
	 * @param m 3x3 column matrix
	 * @return true if representing a rotational matrix, false otherwise
	 */
	public boolean isRotationMatrix(float[] m) {
		double epsilon = 0.01; // margin to allow for rounding errors
		if (MathFloat.abs(m[0]*m[3] + m[3]*m[4] + m[6]*m[7]) > epsilon) return false;
		if (MathFloat.abs(m[0]*m[2] + m[3]*m[5] + m[6]*m[8]) > epsilon) return false;
		if (MathFloat.abs(m[1]*m[2] + m[4]*m[5] + m[7]*m[8]) > epsilon) return false;
		if (MathFloat.abs(m[0]*m[0] + m[3]*m[3] + m[6]*m[6] - 1) > epsilon) return false;
		if (MathFloat.abs(m[1]*m[1] + m[4]*m[4] + m[7]*m[7] - 1) > epsilon) return false;
		if (MathFloat.abs(m[2]*m[2] + m[5]*m[5] + m[8]*m[8] - 1) > epsilon) return false;
		return (MathFloat.abs(determinant(m)-1) < epsilon);
	}
	private float determinant(float[] m) {
	      return m[0]*m[4]*m[8] + m[3]*m[7]*m[2] + m[6]*m[1]*m[5] - m[0]*m[7]*m[5] - m[3]*m[1]*m[8] - m[6]*m[4]*m[2];
	}
}
