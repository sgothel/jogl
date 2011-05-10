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
package com.jogamp.graph.curve;

import java.util.ArrayList;
import java.util.Collections;


import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Outline;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.math.VectorUtil;

import com.jogamp.graph.curve.tess.CDTriangulator2D;

/** A Generic shape objects which is defined by a list of Outlines.
 * This Shape can be transformed to Triangulations.
 * The list of triangles generated are render-able by a Region object.
 * The triangulation produced by this Shape will define the 
 * closed region defined by the outlines.
 * 
 * One or more OutlineShape Object can be associated to a region
 * this is left as a high-level representation of the Objects. For
 * optimizations, flexibility requirements for future features.
 * 
 * <br><br>
 * Example to creating an Outline Shape:
 * <pre>
      addVertex(...)
      addVertex(...)
      addVertex(...)
      addEmptyOutline()
      addVertex(...)
      addVertex(...)
      addVertex(...)
 * </pre>
 * 
 * The above will create two outlines each with three vertices. By adding these two outlines to 
 * the OutlineShape, we are stating that the combination of the two outlines represent the shape.
 * <br>
 * 
 * To specify that the shape is curved at a region, the on-curve flag should be set to false 
 * for the vertex that is in the middle of the curved region (if the curved region is defined by 3
 * vertices (quadratic curve).
 * <br>
 * In case the curved region is defined by 4 or more vertices the middle vertices should both have 
 * the on-curve flag set to false.
 * 
 * <br>Example: <br>
 * <pre>
      addVertex(0,0, true);
      addVertex(0,1, false);
      addVertex(1,1, false);
      addVertex(1,0, true);
 * </pre>
 * 
 * The above snippet defines a cubic nurbs curve where (0,1 and 1,1) 
 * do not belong to the final rendered shape.
 *  
 * <i>Implementation Notes:</i><br>
 * <ul>
 *    <li> The first vertex of any outline belonging to the shape should be on-curve</li>
 *    <li> Intersections between off-curved parts of the outline is not handled</li>
 * </ul>
 * 
 * @see Outline
 * @see Region
 */
public class OutlineShape implements Comparable<OutlineShape> {
  /**
    * Outline's vertices have undefined state until transformed.
    */
    public enum VerticesState {
        UNDEFINED(0), QUADRATIC_NURBS(1);

        public final int state;

        VerticesState(int state){
            this.state = state;
        }
    } 
    
    public static final int DIRTY_BOUNDS = 1 << 0;
    
    private final Vertex.Factory<? extends Vertex> vertexFactory;
    private VerticesState outlineState;

    /** The list of {@link Outline}s that are part of this 
     *  outline shape.
     */
    private ArrayList<Outline> outlines;
    private AABBox bbox;

    /** dirty bits DIRTY_BOUNDS */
    private int dirtyBits;  
    
    /** Create a new Outline based Shape
     */
    public OutlineShape(Vertex.Factory<? extends Vertex> factory) {
        this.vertexFactory = factory;
        this.outlines = new ArrayList<Outline>(3);
        this.outlines.add(new Outline());
        this.outlineState = VerticesState.UNDEFINED;
        this.bbox = new AABBox();
        this.dirtyBits = 0;    
    }

    /** Clears all data and reset all states as if this instance was newly created */
    public void clear() {
        outlines.clear();
        outlines.add(new Outline());
        outlineState = VerticesState.UNDEFINED;
        bbox.reset();
        dirtyBits = 0;    
    }
    
    /** Returns the associated vertex factory of this outline shape
     * @return Vertex.Factory object
     */
    public final Vertex.Factory<? extends Vertex> vertexFactory() { return vertexFactory; }

    public int getOutlineNumber() {
        return outlines.size();
    }
    
    /** Add a new empty {@link Outline} 
     * to the end of this shape's outline list.
     * <p>If the {@link #getLastOutline()} is empty already, no new one will be added.</p>
     * 
     * After a call to this function all new vertices added
     * will belong to the new outline
     */
    public void addEmptyOutline(){
        if( !getLastOutline().isEmpty() ) {
            outlines.add(new Outline());
        }
    }

    /** Appends the {@link Outline} element to the end,
     * ensuring a clean tail.
     * 
     * <p>A clean tail is ensured, no double empty Outlines are produced
     * and a pre-existing empty outline will be replaced with the given one. </p>
     * 
     * @param outline Outline object to be added
     * @throws NullPointerException if the  {@link Outline} element is null 
     */
    public void addOutline(Outline outline) throws NullPointerException {
        addOutline(outlines.size(), outline);
    }

    /** Insert the {@link Outline} element at the given {@code position}.
     * 
     * <p>If the {@code position} indicates the end of this list,
     * a clean tail is ensured, no double empty Outlines are produced
     * and a pre-existing empty outline will be replaced with the given one. </p>
     * 
     * @param position of the added Outline
     * @param outline Outline object to be added
     * @throws NullPointerException if the  {@link Outline} element is null 
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position > getOutlineNumber())
     */
    public void addOutline(int position, Outline outline) throws NullPointerException, IndexOutOfBoundsException {
        if (null == outline) {
            throw new NullPointerException("outline is null");
        }
        if( outlines.size() == position ) {
            final Outline lastOutline = getLastOutline();
            if( outline.isEmpty() && lastOutline.isEmpty() ) {
                return;
            }
            if( lastOutline.isEmpty() ) {
                outlines.set(position-1, outline);
                if( 0 == ( dirtyBits & DIRTY_BOUNDS ) ) {
                    bbox.resize(outline.getBounds());
                }
                return;
            }
        }
        outlines.add(position, outline);
        if( 0 == ( dirtyBits & DIRTY_BOUNDS ) ) {
            bbox.resize(outline.getBounds());
        }
    }

    /** Insert the {@link OutlineShape} elements of type {@link Outline}, .. at the end of this shape,
     * using {@link #addOutline(Outline)} for each element.
     * <p>Closes the current last outline via {@link #closeLastOutline()} before adding the new ones.</p>
     * @param outlineShape OutlineShape elements to be added.
     * @throws NullPointerException if the  {@link OutlineShape} is null 
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position > getOutlineNumber())
     */
    public void addOutlineShape(OutlineShape outlineShape) throws NullPointerException {
        if (null == outlineShape) {
            throw new NullPointerException("OutlineShape is null");
        }
        closeLastOutline();
        for(int i=0; i<outlineShape.getOutlineNumber(); i++) {
            addOutline(outlineShape.getOutline(i));
        }
    }
        
    /** Replaces the {@link Outline} element at the given {@code position}.
     * <p>Sets the bounding box dirty, hence a next call to {@link #getBounds()} will validate it.</p>
     * 
     * @param position of the replaced Outline
     * @param outline replacement Outline object 
     * @throws NullPointerException if the  {@link Outline} element is null 
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position >= getOutlineNumber())
     */
    public void setOutline(int position, Outline outline) throws NullPointerException, IndexOutOfBoundsException {
        if (null == outline) {
            throw new NullPointerException("outline is null");
        }
        outlines.set(position, outline);
        dirtyBits |= DIRTY_BOUNDS;
    }
    
    /** Removes the {@link Outline} element at the given {@code position}.
     * <p>Sets the bounding box dirty, hence a next call to {@link #getBounds()} will validate it.</p>
     * 
     * @param position of the to be removed Outline
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position >= getOutlineNumber())
     */
    public final Outline removeOutline(int position) throws IndexOutOfBoundsException {
        dirtyBits |= DIRTY_BOUNDS;
        return outlines.remove(position);
    }
        
    /** Get the last added outline to the list
     * of outlines that define the shape
     * @return the last outline
     */
    public final Outline getLastOutline(){
        return outlines.get(outlines.size()-1);
    }
    
    /** @return the {@code Outline} at {@code position} 
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position >= getOutlineNumber())
     */
    public Outline getOutline(int position) throws IndexOutOfBoundsException {
        return outlines.get(position);
    }    
        
    /** Adds a vertex to the last open outline in the
     *  shape. 
     * @param v the vertex to be added to the OutlineShape
     */
    public final void addVertex(Vertex v){
        final Outline lo = getLastOutline();
        lo.addVertex(v);
        if( 0 == ( dirtyBits & DIRTY_BOUNDS ) ) {
            bbox.resize(lo.getBounds());
        }
    }
    
    /** Adds a vertex to the last open outline in the shape. 
     * at {@code position} 
     * @param position indx at which the vertex will be added 
     * @param v the vertex to be added to the OutlineShape
     */
    public final void addVertex(int position, Vertex v){
        final Outline lo = getLastOutline();
        lo.addVertex(position, v);
        if( 0 == ( dirtyBits & DIRTY_BOUNDS ) ) {
            bbox.resize(lo.getBounds());
        }
    }

    /** Add a 2D {@link Vertex} to the last outline by defining the coordniate attribute
     * of the vertex. The 2D vertex will be represented as Z=0.
     * 
     * @param x the x coordinate
     * @param y the y coordniate
     * @param onCurve flag if this vertex is on the final curve or defines a curved region
     * of the shape around this vertex.
     */
    public final void addVertex(float x, float y, boolean onCurve) {
        addVertex(vertexFactory.create(x, y, 0f, onCurve));
    }

    /** Add a 3D {@link Vertex} to the last outline by defining the coordniate attribute
     * of the vertex.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param onCurve flag if this vertex is on the final curve or defines a curved region
     * of the shape around this vertex.
     */
    public final void addVertex(float x, float y, float z, boolean onCurve) {
        addVertex(vertexFactory.create(x, y, z, onCurve));
    }

    /** Add a vertex to the last outline by passing a float array and specifying the 
     * offset and length in which. The attributes of the vertex are located. 
     * The attributes should be continuous (stride = 0).
     * Attributes which value are not set (when length less than 3) 
     * are set implicitly to zero.
     * @param coordsBuffer the coordinate array where the vertex attributes are to be picked from
     * @param offset the offset in the buffer to the x coordinate
     * @param length the number of attributes to pick from the buffer (maximum 3)
     * @param onCurve flag if this vertex is on the final curve or defines a curved region
     * of the shape around this vertex.
     */
    public final void addVertex(float[] coordsBuffer, int offset, int length, boolean onCurve) {
        addVertex(vertexFactory.create(coordsBuffer, offset, length, onCurve));
    }    

    /** Closes the last outline in the shape.
     * <p>If last vertex is not equal to first vertex.
     * A new temp vertex is added at the end which 
     * is equal to the first.</p>
     */
    public void closeLastOutline(){
        getLastOutline().setClosed(true);
    }

    /**
     * @return the outline's vertices state, {@link OutlineShape.VerticesState}
     */
    public final VerticesState getOutlineState() {
        return outlineState;
    }
        
    /** Ensure the outlines represent
     * the specified destinationType.
     * 
     * @param destinationType the target outline's vertices state. Currently only {@link OutlineShape.VerticesState#QUADRATIC_NURBS} are supported.
     */
    public void transformOutlines(VerticesState destinationType){
        if(outlineState != destinationType){
            if(destinationType == VerticesState.QUADRATIC_NURBS){
                transformOutlines2Quadratic();
            } else {
                throw new IllegalStateException("destinationType "+destinationType.name()+" not supported (currently "+outlineState.name()+")");
            }
        }
    }

    private void transformOutlines2Quadratic(){
        final int count = getOutlineNumber();
        for (int cc = 0; cc < count; cc++){            
            final Outline outline = getOutline(cc);
            int vertexNumberLessOne = outline.getVertexNumber() - 1;
            for(int i=0; i < vertexNumberLessOne; i++) {
                final Vertex currentVertex = outline.getVertex(i);
                final Vertex nextVertex = outline.getVertex(i+1);
                if ( !currentVertex.isOnCurve() && !nextVertex.isOnCurve() ) {
                    final float[] newCoords = VectorUtil.mid(currentVertex.getCoord(), nextVertex.getCoord());
                    final Vertex v = vertexFactory.create(newCoords, 0, 3, true);
                    v.setOnCurve(true);                    
                    i++;
                    vertexNumberLessOne++;
                    outline.addVertex(i, v);
                }                
            }
            // Cut off last vertex (which is on-curve)
            // FIXME: original code skipped the last element (it _is_ unrelated to the xform above)
            // FIXME: understand why the last element produces artifacts in rendering
            if( vertexNumberLessOne >= 0 ) {
                outline.removeVertex(vertexNumberLessOne);
            }                
        }
        outlineState = VerticesState.QUADRATIC_NURBS;
    }

    private void generateVertexIds(){
        int maxVertexId = 0;
        for(int i=0; i<outlines.size(); i++) {
            final ArrayList<Vertex> vertices = outlines.get(i).getVertices();
            for(Vertex vert:vertices){
                vert.setId(maxVertexId);
                maxVertexId++;
            }
        }
    }

    /** @return the list of concatenated vertices associated with all 
     * {@code Outline}s of this object
     */
    public ArrayList<Vertex> getVertices() {
        ArrayList<Vertex> vertices = new ArrayList<Vertex>();
        for(int i=0; i<outlines.size(); i++) {
            vertices.addAll(outlines.get(i).getVertices());
        }
        return vertices;
    }

    /**
     * Triangulate the {@link OutlineShape} generating a list of triangles
     * @return an arraylist of triangles representing the filled region
     * which is produced by the combination of the outlines
     */
    public ArrayList<Triangle> triangulate() {
        if(outlines.size() == 0){
            return null;
        }
        sortOutlines();
        generateVertexIds();

        CDTriangulator2D triangulator2d = new CDTriangulator2D();
        for(int index = 0; index<outlines.size(); index++) {
            triangulator2d.addCurve(outlines.get(index));
        }
        
        ArrayList<Triangle> triangles = triangulator2d.generateTriangulation();
        triangulator2d.reset();

        return triangles;
    }

    /** Sort the outlines from large
     *  to small depending on the AABox
     */
    private void sortOutlines() {
        Collections.sort(outlines);
        Collections.reverse(outlines);
    }
    
    /** Compare two outline shapes with Bounding Box area
     * as criteria. 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public final int compareTo(OutlineShape outline) {
        float size = getBounds().getSize();
        float newSize = outline.getBounds().getSize();
        if(size < newSize){
            return -1;
        }
        else if(size > newSize){
            return 1;
        }
        return 0;
    }
    
    private final void validateBoundingBox() {
        dirtyBits &= ~DIRTY_BOUNDS;
        bbox.reset();
        for (int i=0; i<outlines.size(); i++) {
            bbox.resize(outlines.get(i).getBounds());
        }
    }
         
    public final AABBox getBounds() {
        if( 0 == ( dirtyBits & DIRTY_BOUNDS ) ) {
            validateBoundingBox();
        }
        return bbox;
    }    

    /**
     * @param obj the Object to compare this OutlineShape with
     * @return true if {@code obj} is an OutlineShape, not null, 
     *                 same outlineState, equal bounds and equal outlines in the same order 
     */
    public boolean equals(Object obj) {
        if( obj == this) {
            return true;
        }
        if( null == obj || !(obj instanceof OutlineShape) ) {
            return false;
        }        
        final OutlineShape o = (OutlineShape) obj;
        if(getOutlineState() != o.getOutlineState()) {
            return false;
        }
        if(getOutlineNumber() != o.getOutlineNumber()) {
            return false;
        }
        if( !getBounds().equals( o.getBounds() ) ) {
            return false;
        }
        for (int i=getOutlineNumber()-1; i>=0; i--) {
            if( ! getOutline(i).equals( o.getOutline(i) ) ) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * @return deep clone of this OutlineShape w/o Region
     */
    public OutlineShape clone() {
        OutlineShape o;
        try {
            o = (OutlineShape) super.clone();
        } catch (CloneNotSupportedException e) { throw new InternalError(); }
        o.bbox = bbox.clone();
        o.outlines = new ArrayList<Outline>(outlines.size());
        for(int i=0; i<outlines.size(); i++) {
            o.outlines.add(outlines.get(i).clone());
        }
        return o;
    }                
}
