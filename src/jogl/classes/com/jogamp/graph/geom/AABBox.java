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
package com.jogamp.graph.geom;

import com.jogamp.graph.math.VectorUtil;

/**
 * Axis Aligned Bounding Box. Defined by two 3D coordinates (low and high)
 * The low being the the lower left corner of the box, and the high being the upper 
 * right corner of the box.
 * 
 */
public class AABBox implements Cloneable {
    private float[] low = new float[3];
    private float[] high = new float[3];
    private float[] center = new float[3];

    /** Create a Axis Aligned bounding box (AABBox) 
     * where the low and and high MAX float Values.
     */
    public AABBox() {
        reset();
    }

    /** Create an AABBox specifying the coordinates 
     * of the low and high
     * @param lx min x-coordinate
     * @param ly min y-coordnate
     * @param lz min z-coordinate
     * @param hx max x-coordinate
     * @param hy max y-coordinate
     * @param hz max z-coordinate
     */
    public AABBox(float lx, float ly, float lz,
            float hx, float hy, float hz)
    {
        reset();
        resize(lx, ly, lz);
        resize(hx, hy, hz);

        computeCenter();
    }
    
    /** Create a AABBox defining the low and high
     * @param low min xyz-coordinates
     * @param high max xyz-coordinates
     */
    public AABBox(float[] low, float[] high) {
        reset();
        resize(low[0],low[1],low[2]);
        resize(high[0],high[1],high[2]);

        computeCenter();
    }

    /** resets this box to the inverse low/high, allowing the next {@link #resize(float, float, float)} command to hit. */
    public final void reset() {
        setLow(Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE);
        setHigh(-1*Float.MAX_VALUE,-1*Float.MAX_VALUE,-1*Float.MAX_VALUE);
        center[0] = 0f;
        center[1] = 0f;
        center[2] = 0f;
    }
    
    /** Get the max xyz-coordinates
     * @return a float array containing the max xyz coordinates
     */
    public final float[] getHigh() {
        return high;
    }
    
    private final void setHigh(float hx, float hy, float hz) {
        this.high[0] = hx;
        this.high[1] = hy;
        this.high[2] = hz;
    }
    
    /** Get the min xyz-coordinates
     * @return a float array containing the min xyz coordinates
     */
    public final float[] getLow() {
        return low;
    }
    
    private final void setLow(float lx, float ly, float lz) {
        this.low[0] = lx;
        this.low[1] = ly;
        this.low[2] = lz;
    }

    /** Resize the AABBox to encapsulate another AABox
     * @param newBox AABBox to be encapsulated in
     */
    public final void resize(AABBox newBox) {
        float[] newLow = newBox.getLow();
        float[] newHigh = newBox.getHigh();

        /** test low */
        if (newLow[0] < low[0])
            low[0] = newLow[0];
        if (newLow[1] < low[1])
            low[1] = newLow[1];
        if (newLow[2] < low[2])
            low[2] = newLow[2];

        /** test high */
        if (newHigh[0] > high[0])
            high[0] = newHigh[0];
        if (newHigh[1] > high[1])
            high[1] = newHigh[1];
        if (newHigh[2] > high[2])
            high[2] = newHigh[2];

        computeCenter();
    }

    private final void computeCenter() {
        center[0] = (high[0] + low[0])/2;
        center[1] = (high[1] + low[1])/2;
        center[2] = (high[2] + low[2])/2;
    }

    /** Resize the AABBox to encapsulate the passed
     * xyz-coordinates. 
     * @param x x-axis coordinate value
     * @param y y-axis coordinate value
     * @param z z-axis coordinate value
     */
    public final void resize(float x, float y, float z) {    
        /** test low */
        if (x < low[0])
            low[0] = x;
        if (y < low[1])
            low[1] = y;
        if (z < low[2])
            low[2] = z;

        /** test high */
        if (x > high[0])
            high[0] = x;
        if (y > high[1])
            high[1] = y;
        if (z > high[2])
            high[2] = z;
        
        computeCenter();
    }

    /** Resize the AABBox to encapsulate the passed
     * xyz-coordinates. 
     * @param xyz xyz-axis coordinate values
     * @param offset of the array
     */
    public final void resize(float[] xyz, int offset) {
        resize(xyz[0+offset], xyz[1+offset], xyz[2+offset]);
    }

    /** Check if the x & y coordinates are bounded/contained
     *  by this AABBox
     * @param x  x-axis coordinate value
     * @param y  y-axis coordinate value
     * @return true if  x belong to (low.x, high.x) and
     * y belong to (low.y, high.y)
     */
    public final boolean contains(float x, float y) {
        if(x<low[0] || x>high[0]){
            return false;
        }
        if(y<low[1]|| y>high[1]){
            return false;
        }
        return true;
    }
    
    /** Check if the xyz coordinates are bounded/contained
     *  by this AABBox.
     * @param x x-axis coordinate value
     * @param y y-axis coordinate value
     * @param z z-axis coordinate value
     * @return true if  x belong to (low.x, high.x) and
     * y belong to (low.y, high.y) and  z belong to (low.z, high.z)
     */
    public final boolean contains(float x, float y, float z) {
        if(x<low[0] || x>high[0]){
            return false;
        }
        if(y<low[1]|| y>high[1]){
            return false;
        }
        if(z<low[2] || z>high[2]){
            return false;
        }
        return true;
    }
    
    /** Check if there is a common region between this AABBox and the passed
     *     2D region irrespective of z range
     * @param x lower left x-coord
     * @param y lower left y-coord
     * @param w width
     * @param h hight
     * @return true if this AABBox might have a common region with this 2D region
     */
    public final boolean intersects(float x, float y, float w, float h) {
        if (w <= 0 || h <= 0) {
            return false;
        }
        
        final float _w = getWidth();
        final float _h = getHeight();        
        if (_w <= 0 || _h <= 0) {
            return false;
        }
        
        final float x0 = getMinX();
        final float y0 = getMinY();
        return (x + w > x0 &&
                y + h > y0 &&
                x < x0 + _w &&
                y < y0 + _h);
    }

    
    /** Get the size of the Box where the size is represented by the 
     * length of the vector between low and high.
     * @return a float representing the size of the AABBox
     */
    public final float getSize() {
        return VectorUtil.computeLength(low, high);
    }

    /**Get the Center of the AABBox
     * @return the xyz-coordinates of the center of the AABBox
     */
    public final float[] getCenter() {
        return center;
    }

    /** Scale the AABBox by a constant
     * @param size a constant float value
     */
    public final void scale(float size) {
        float[] diffH = new float[3];
        diffH[0] = high[0] - center[0];
        diffH[1] = high[1] - center[1];
        diffH[2] = high[2] - center[2];
        
        diffH = VectorUtil.scale(diffH, size);
        
        float[] diffL = new float[3];
        diffL[0] = low[0] - center[0];
        diffL[1] = low[1] - center[1];
        diffL[2] = low[2] - center[2];
        
        diffL = VectorUtil.scale(diffL, size);
        
        high = VectorUtil.vectorAdd(center, diffH);
        low = VectorUtil.vectorAdd(center, diffL);
    }

    public final float getMinX() {
        return low[0];
    }
    
    public final float getMinY() {
        return low[1];
    }
    
    public final float getWidth(){
        return high[0] - low[0];
    }
    
    public final float getHeight() {
        return high[1] - low[1];
    }
    
    public final float getDepth() {
        return high[2] - low[2];
    }
    
    public final AABBox clone() {
        return new AABBox(this.low, this.high);
    }
    
    public final boolean equals(Object obj) {
        if( obj == this ) {
            return true;
        }
        if( null == obj || !(obj instanceof AABBox) ) {
            return false;
        }
        final AABBox other = (AABBox) obj; 
        return VectorUtil.checkEquality(low, other.low) &&          
               VectorUtil.checkEquality(high, other.high) ;
    }
    
    public final String toString() {
        return "[ "+low[0]+"/"+low[1]+"/"+low[1]+" .. "+high[0]+"/"+high[0]+"/"+high[0]+", ctr "+
                    center[0]+"/"+center[1]+"/"+center[1]+" ]";
    }
}
