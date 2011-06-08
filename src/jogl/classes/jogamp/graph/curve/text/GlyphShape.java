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

import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex.Factory;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.math.Quaternion;

public class GlyphShape {
    
    private Quaternion quat= null;
    private OutlineShape shape = null;
    
    /** Create a new Glyph shape
     * based on Parametric curve control polyline
     */
    public GlyphShape(Vertex.Factory<? extends Vertex> factory){
        shape = new OutlineShape(factory);
    }
    
    /** Create a new GlyphShape from a {@link OutlineShape}
     * @param factory vertex impl factory {@link Factory}
     * @param shape {@link OutlineShape} representation of the Glyph
     */
    public GlyphShape(Vertex.Factory<? extends Vertex> factory, OutlineShape shape){
        this(factory);
        this.shape = shape;
        this.shape.transformOutlines(OutlineShape.VerticesState.QUADRATIC_NURBS);
    }
    
    public final Vertex.Factory<? extends Vertex> vertexFactory() { return shape.vertexFactory(); }
    
    public OutlineShape getShape() {
        return shape;
    }
    
    public int getNumVertices() {
        return shape.getVertices().size();
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
    
    /** Triangluate the glyph shape
     * @return ArrayList of triangles which define this shape
     */
    public ArrayList<Triangle> triangulate(){
        return shape.triangulate();
    }

    /** Get the list of Vertices of this Object
     * @return arrayList of Vertices
     */
    public ArrayList<Vertex> getVertices(){
        return shape.getVertices();
    }    
}
