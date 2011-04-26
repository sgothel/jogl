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
package com.jogamp.graph.curve.opengl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.media.opengl.GL2ES2;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.RegionFactory;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;

public abstract class RegionRenderer extends Renderer {

    /** 
     * Create a Hardware accelerated Text Renderer.
     * @param rs the used {@link RenderState} 
     * @param renderType either {@link com.jogamp.graph.curve.Region#SINGLE_PASS} or {@link com.jogamp.graph.curve.Region#TWO_PASS}
     */
    public static RegionRenderer create(RenderState rs, int type) {
        return new jogamp.graph.curve.opengl.RegionRendererImpl01(rs, type);
    }
    
    protected RegionRenderer(RenderState rs, int type) {
        super(rs, type);
    }
    
    /** Render an array of {@link OutlineShape}s combined in one region
     *  at the position provided the triangles of the 
     *  shapes will be generated, if not yet generated
     * @param outlineShapes array of OutlineShapes to Render.
     * @param position the initial translation of the outlineShapes.
     * @param texSize texture size for multipass render     * 
     * @throws Exception if HwRegionRenderer not initialized
     */
    public abstract void renderOutlineShapes(GL2ES2 gl, OutlineShape[] outlineShapes, float[] position, int texSize);

    /** Render an {@link OutlineShape} in 3D space at the position provided
     *  the triangles of the shapes will be generated, if not yet generated
     * @param outlineShape the OutlineShape to Render.
     * @param position the initial translation of the outlineShape. 
     * @param texSize texture size for multipass render
     * @throws Exception if HwRegionRenderer not initialized
     */
    public abstract void renderOutlineShape(GL2ES2 gl, OutlineShape outlineShape, float[] position, int texSize);

    protected HashMap<Integer, Region> regions = new HashMap<Integer, Region>();

    public void flushCache(GL2ES2 gl) {
        Iterator<Region> iterator = regions.values().iterator();
        while(iterator.hasNext()){
            Region region = iterator.next();
            region.destroy(gl, rs);
        }
        regions.clear();
    }       

    @Override
    protected void disposeImpl(GL2ES2 gl) {
        // fluchCache(gl) already called
    }
    
    /** 
     * Create an ogl {@link Region} defining this {@link OutlineShape}
     * @return the resulting Region.
     */
    protected Region createRegion(GL2ES2 gl, OutlineShape outlineShape) {
        Region region = RegionFactory.create(rs, renderType);
        
        outlineShape.transformOutlines(OutlineShape.QUADRATIC_NURBS);
        ArrayList<Triangle> triangles = (ArrayList<Triangle>) outlineShape.triangulate(rs.getSharpness().floatValue());
        ArrayList<Vertex> vertices = (ArrayList<Vertex>) outlineShape.getVertices();
        region.addVertices(vertices);
        region.addTriangles(triangles);
        
        region.update(gl);
        return region;
    }
    
    /** Create an ogl {@link Region} defining the list of {@link OutlineShape}.
     * Combining the Shapes into single buffers.
     * @return the resulting Region inclusive the generated region
     */
    protected Region createRegion(GL2ES2 gl, OutlineShape[] outlineShapes) {
        Region region = RegionFactory.create(rs, renderType);
        
        int numVertices = region.getNumVertices();
        
        for(OutlineShape outlineShape:outlineShapes){
            outlineShape.transformOutlines(OutlineShape.QUADRATIC_NURBS);

            ArrayList<Triangle> triangles = outlineShape.triangulate(rs.getSharpness().floatValue());
            region.addTriangles(triangles);
            
            ArrayList<Vertex> vertices = outlineShape.getVertices();
            for(Vertex vert:vertices){
                vert.setId(numVertices++);
            }
            region.addVertices(vertices);
        }
        
        region.update(gl);
        return region;
    }
    
    protected static int getHashCode(OutlineShape outlineShape){
        return outlineShape.hashCode();
    }
    
    protected static int getHashCode(OutlineShape[] outlineShapes){
        int hashcode = 0;
        for(OutlineShape outlineShape:outlineShapes){
            hashcode += getHashCode(outlineShape);
        }
        return hashcode;
    }       
}