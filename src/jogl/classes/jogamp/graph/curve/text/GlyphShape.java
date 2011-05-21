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
package jogamp.graph.curve.text;

import java.util.ArrayList;

import jogamp.graph.geom.plane.PathIterator;
import static jogamp.graph.geom.plane.PathIterator.*;

import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Triangle;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.math.Quaternion;

/**
 * 
 */
public class GlyphShape
    extends OutlineShape
{
    
    private Quaternion quat= null;
    private int numVertices = 0;

    
    /** Create a new Glyph shape
     * based on Parametric curve control polyline
     */
    public GlyphShape(Vertex.Factory<? extends Vertex> factory){
        super(factory);
    }
    
    /** Create a GlyphShape from a font Path Iterator
     * @param pathIterator the path iterator
     * 
     * @see PathIterator
     */
    public GlyphShape(Vertex.Factory<? extends Vertex> factory, PathIterator pathIterator){
        this(factory);
        
        if(null != pathIterator){
            while(!pathIterator.isDone()){
                float[] coords = new float[6];
                int segmentType = pathIterator.currentSegment(coords);
                this.addOutlineVerticesFromGlyphVector(coords, segmentType);

                pathIterator.next();
            }
        }
        this.transformOutlines(OutlineShape.VerticesState.QUADRATIC_NURBS);
    }
    

    
    private void addVertexToLastOutline(Vertex vertex){
    	//FIXME: assuming font outline comes in CW order
        this.addVertex(vertex);
    }
    
    private void addOutlineVerticesFromGlyphVector(float[] coords, int segmentType){
        switch(segmentType) {
            case PathIterator.SEG_MOVETO:
                addEmptyOutlineIfNot();
                addVertexToLastOutline(vertexFactory().create(coords, 0, 2, true));            
                numVertices++;
                break;
            case PathIterator.SEG_LINETO:
                addVertexToLastOutline(vertexFactory().create(coords, 0, 2, true));            
                numVertices++;
                break;
            case PathIterator.SEG_QUADTO:
                addVertexToLastOutline(vertexFactory().create(coords, 0, 2, false));
                addVertexToLastOutline(vertexFactory().create(coords, 2, 2, true));            
                numVertices+=2;
                break;
            case PathIterator.SEG_CUBICTO:
                addVertexToLastOutline(vertexFactory().create(coords, 0, 2, false));
                addVertexToLastOutline(vertexFactory().create(coords, 2, 2, false));
                addVertexToLastOutline(vertexFactory().create(coords, 4, 2, true));            
                numVertices+=3;
                break;
            case PathIterator.SEG_CLOSE:
                closeLastOutline();
                break;
            default:
                throw new IllegalArgumentException("Unhandled Segment Type: "+segmentType);
        }
    }
    
    public OutlineShape getShape() {
        return this;
    }
    
    public int getNumVertices() {
        return numVertices;
    }
    
    /** Get the rotational Quaternion attached to this Shape
     * @return the Quaternion Object
     */
    public Quaternion getQuat() {
        return quat;
    }
    
    /** Set the Quaternion that shall defien the rotation
     * of this shape.
     * @param quat
     */
    public void setQuat(Quaternion quat) {
        this.quat = quat;
    }

    public GlyphShape clone(){
        GlyphShape clone = (GlyphShape)super.clone();
        if (null != clone.quat)
            clone.quat = clone.quat.clone();
        return clone;
    }

    /**
     * Clear and reinitialize.  Drop any quaternion.
     */
    @Override
    public void clear(){
        this.quat = null;
        this.numVertices = 0;
        super.clear();
    }
}
