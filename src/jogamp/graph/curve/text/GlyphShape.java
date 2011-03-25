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

import com.jogamp.graph.geom.plane.PathIterator;
import com.jogamp.graph.geom.Line;
import com.jogamp.graph.geom.Point;
import com.jogamp.graph.geom.PointTex;
import com.jogamp.graph.geom.Triangle;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.math.Quaternion;

public class GlyphShape {
	
	private Quaternion quat= null;
	private int numVertices = 0;
	private OutlineShape shape = null;
	
	/** Create a new Glyph shape
	 * based on Parametric curve control polyline
	 */
	public GlyphShape(Point.Factory<? extends PointTex> factory){
		shape = new OutlineShape(factory);
	}
	
	/** Create a GlyphShape from a font Path Iterator
	 * @param pathIterator the path iterator
	 * 
	 * @see PathIterator
	 */
	public GlyphShape(Point.Factory<? extends PointTex> factory, PathIterator pathIterator){
		this(factory);
		
		if(null != pathIterator){
			while(!pathIterator.isDone()){
				float[] coords = new float[6];
				int segmentType = pathIterator.currentSegment(coords);
				addOutlineVerticesFromGlyphVector(coords, segmentType);

				pathIterator.next();
			}
		}
		shape.transformOutlines(OutlineShape.QUADRATIC_NURBS);
	}
	
	public final Point.Factory<? extends PointTex> pointFactory() { return shape.pointFactory(); }
	
	private void addVertexToLastOutline(PointTex vertex){
		shape.addVertex(vertex);
	}
	
	private void addOutlineVerticesFromGlyphVector(float[] coords, int segmentType){
		if(segmentType == PathIterator.SEG_MOVETO){
			if(!shape.getLastOutline().isEmpty()){
				shape.addEmptyOutline();
			}			
			PointTex vert = pointFactory().create(coords[0],coords[1]);
			vert.setOnCurve(true);
			addVertexToLastOutline(vert);
			
			numVertices++;
		}
		else if(segmentType == PathIterator.SEG_LINETO){
			PointTex vert1 = pointFactory().create(coords[0],coords[1]);
			vert1.setOnCurve(true);
			addVertexToLastOutline(vert1);
			
			numVertices++;
		}
		else if(segmentType == PathIterator.SEG_QUADTO){
			PointTex vert1 = pointFactory().create(coords[0],coords[1]);
			vert1.setOnCurve(false);
			addVertexToLastOutline(vert1);

			PointTex vert2 = pointFactory().create(coords[2],coords[3]);
			vert2.setOnCurve(true);
			addVertexToLastOutline(vert2);
			
			numVertices+=2;
		}
		else if(segmentType == PathIterator.SEG_CUBICTO){
			PointTex vert1 = pointFactory().create(coords[0],coords[1]);
			vert1.setOnCurve(false);
			addVertexToLastOutline(vert1);

			PointTex vert2 = pointFactory().create(coords[2],coords[3]);
			vert2.setOnCurve(false);
			addVertexToLastOutline(vert2);

			PointTex vert3 = pointFactory().create(coords[4],coords[5]);
			vert3.setOnCurve(true);
			addVertexToLastOutline(vert3);
			
			numVertices+=3;
		}
		else if(segmentType == PathIterator.SEG_CLOSE){
			shape.closeLastOutline();
		}
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
	
	/** Triangluate the glyph shape
	 * @param sharpness sharpness of the curved regions default = 0.5
	 * @return ArrayList of triangles which define this shape
	 */
	public ArrayList<Triangle<PointTex>> triangulate(float sharpness){
		return shape.triangulate(sharpness);
	}

	/** Get the list of Vertices of this Object
	 * @return arrayList of Vertices
	 */
	public ArrayList<PointTex> getVertices(){
		return shape.getVertices();
	}
	
	/** Get the list of AA lines defined by this object
	 * @return arraylist of lines
	 */
	public ArrayList<Line<PointTex>> getLines(){
		return shape.getLines();
	}
}
