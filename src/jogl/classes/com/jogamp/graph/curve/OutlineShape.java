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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
public class OutlineShape
    extends ArrayList<Outline>
{
    /**
     * Outline has initial user state (vertices) until transformed.
     */
    public enum VerticesState {
        Init(false), NURBS(true);

        public final boolean transformed;

        VerticesState(boolean transformed){
            this.transformed = transformed;
        }
    }


    private final Vertex.Factory<? extends Vertex> vertexFactory;

    private VerticesState state = VerticesState.Init;


    /** Create a new Outline based Shape
     */
    public OutlineShape(Vertex.Factory<? extends Vertex> factory) {
        super(3);
        if (null != factory){
            this.vertexFactory = factory;
            this.add(new Outline());
        }
        else
            throw new IllegalArgumentException("Missing factory");
    }


    /**
     * @return User vertices, not transformed (to NURBS)
     */
    public boolean hasVerticesUser(){
        return (!this.state.transformed);
    }
    /**
     * @return Transformed vertices
     */
    public boolean hasVerticesTransformed(){
        return this.state.transformed;
    }
    public VerticesState getVerticesState(){
        return this.state;
    }
    public OutlineShape toVerticesStateInit(){
        return this.toVerticesState(VerticesState.Init);
    }
    public OutlineShape toVerticesState(VerticesState state){
        if (null != state){
            this.state = state;
            return this;
        }
        else
            throw new IllegalArgumentException();
    }
    /** Returns the associated vertex factory of this outline shape
     * @return Vertex.Factory object
     */
    public final Vertex.Factory<? extends Vertex> vertexFactory() { return vertexFactory; }

    /** Add a new empty {@link Outline} 
     * to the shape, this new outline will
     * be placed at the end of the outline list.
     * 
     * After a call to this function all new vertices added
     * will belong to the new outline
     */
    public void addEmptyOutline(){
        this.add(new Outline());
    }

    /** Adds an {@link Outline} to the OutlineShape object
     * if last outline of the shape is empty, it will replace
     * that last Outline with the new one. If outline is empty,
     * it will do nothing.
     * @param outline an Outline object
     */
    @Override
    public boolean add(Outline outline){
        if (null != outline){
            boolean mod = false;
            Outline last = this.getLastOutline();
            if (null != last && last.isEmpty()){
                mod = this.remove(last);
            }
            return (super.add(outline) || mod);
        }
        else
            throw new IllegalArgumentException();
    }
    /**
     * Insert
     */
    @Override
    public void add(int index, Outline outline) {
        if (null != outline)
            super.add(index,outline);
        else
            throw new IllegalArgumentException();
    }
    @Override
    public boolean addAll(Collection<? extends Outline> c) {

        return super.addAll(c);
    }
    @Override
    public boolean addAll(int index, Collection<? extends Outline> c) {

        return super.addAll(index,c);
    }
    /**
     * Clear and reinitialize vertices state.
     */
    @Override
    public void clear(){
        this.toVerticesStateInit();
        super.clear();
    }
    public boolean isNotEmpty(){
        return (0 < this.size());
    }
    public boolean replace(Outline outlineOld, Outline outlineNew){
        return (outlineOld == this.set(this.indexOf(outlineOld),outlineNew));
    }
    /** Adds a vertex to the last open outline in the
     *  shape. 
     * @param v the vertex to be added to the OutlineShape
     */
    public final boolean addVertex(Vertex v){

        return getLastOutline().add(v);
    }

    /** Add a 2D {@link Vertex} to the last outline by defining the coordniate attribute
     * of the vertex. The 2D vertex will be represented as Z=0.
     * 
     * @param x the x coordinate
     * @param y the y coordniate
     * @param onCurve flag if this vertex is on the final curve or defines a curved region
     * of the shape around this vertex.
     */
    public final boolean addVertex(float x, float y, boolean onCurve) {

        return getLastOutline().add(vertexFactory, x, y, onCurve);
    }

    /** Add a 3D {@link Vertex} to the last outline by defining the coordniate attribute
     * of the vertex.
     * @param x the x coordinate
     * @param y the y coordniate
     * @param z the z coordniate
     * @param onCurve flag if this vertex is on the final curve or defines a curved region
     * of the shape around this vertex.
     */
    public final boolean addVertex(float x, float y, float z, boolean onCurve) {

        return getLastOutline().add(vertexFactory, x, y, z, onCurve);
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
    public final boolean addVertex(float[] coordsBuffer, int offset, int length, boolean onCurve) {

        return this.getLastOutline().add(vertexFactory, coordsBuffer, offset, length, onCurve);
    }    

    /** Closes the last outline in the shape
     * if last vertex is not equal to first vertex.
     * A new temp vertex is added at the end which 
     * is equal to the first.
     */
    public void closeLastOutline(){
        getLastOutline().setClosed(true);
    }

    /** Get the last added outline to the list
     * of outlines that define the shape
     * @return the last outline
     */
    public final Outline getLastOutline(){
        final int index = (this.size()-1);
        if (-1 < index)
            return this.get(index);
        else
            return null;
    }
    /** Make sure that the outlines represent
     * the specified destinationType, if not
     * transform outlines to destination type.
     * @param destinationType The curve type needed
     */
    public void transformOutlines(VerticesState destinationType){

        if (destinationType != this.state){

            if (destinationType == VerticesState.NURBS){

                transformOutlinesQuadratic();
            }
            else
                throw new IllegalStateException(String.format("Change to VerticesState %s from %s",destinationType.name(),this.state.name()));
        }
    }
    /**
     * Transform in place
     */
    private void transformOutlinesQuadratic(){

        /**loop over the outlines and make sure no
         * adj off-curve vertices
         */
        final int count = this.size();
        for (int cc = 0; cc < count; cc++){
            Outline exiOutline = this.get(cc);
            Outline newOutline = new Outline();

            ArrayList<Vertex> vertices = exiOutline.getVertices();
            int size =vertices.size()-1;
            for(int i=0;i<size;i++){
                Vertex currentVertex = vertices.get(i);
                Vertex nextVertex = vertices.get((i+1)%size);
                if ((!currentVertex.isOnCurve()) && (!nextVertex.isOnCurve())) {
                    newOutline.add(currentVertex);

                    float[] newCoords = VectorUtil.mid(currentVertex.getCoord(), nextVertex.getCoord());
                    newOutline.add(vertexFactory, newCoords, 0, 3, true);
                }
                else {
                    newOutline.add(currentVertex);
                }
            }
            if (!exiOutline.equals(newOutline))
                this.set(cc,newOutline);
        }
        this.toVerticesState(VerticesState.NURBS);
    }

    private void generateVertexIds(){
        int maxVertexId = 0;
        for(Outline outline: this){

            for(Vertex vert: outline){
                vert.setId(maxVertexId);
                maxVertexId++;
            }
        }
    }

    /** @return the list of vertices associated with the 
     * {@code Outline} list of this object
     */
    public ArrayList<Vertex> getVertices(){
        ArrayList<Vertex> vertices = new ArrayList<Vertex>();
        for(Outline polyline: this){
            vertices.addAll(polyline);
        }
        return vertices;
    }

    /** Triangulate the outline shape generating a list of triangles
     * @return an arraylist of triangles representing the filled region
     * which is produced by the combination of the outlines 
     */
    public ArrayList<Triangle> triangulate(){
        return triangulate(0.5f);
    }

    /**Triangulate the {@link OutlineShape} generating a list of triangles
     * @param sharpness defines the curvature strength around the off-curve vertices.
     * defaults to 0.5f
     * @return an arraylist of triangles representing the filled region
     * which is produced by the combination of the outlines
     */
    public ArrayList<Triangle> triangulate(float sharpness){
        if(this.size() == 0){
            return null;
        }
        sortOutlines();
        generateVertexIds();
        
        CDTriangulator2D triangulator2d = new CDTriangulator2D(sharpness);
        for(int index = 0; index< this.size();index++){
            Outline outline = this.get(index);
            triangulator2d.addCurve(outline);
        }
        
        ArrayList<Triangle> triangles = triangulator2d.generateTriangulation();
        triangulator2d.reset();

        return triangles;
    }

    /** Sort the outlines from large
     *  to small depending on the AABox
     */
    private void sortOutlines() {
        Collections.sort(this);
        Collections.reverse(this);
    }
    /** 
     * @return Shallow clone not outlines
     */
    public OutlineShape clone(){
        OutlineShape clone = (OutlineShape)super.clone();
        return clone;
    }
    public AABBox getBounds(){
        /*
         * [TODO] Review this use case for expected frequency
         */
        AABBox bbox = new AABBox();
        for (Outline ol: this){
            bbox.resize(ol.getBounds());
        }
        return bbox;
    }
}
