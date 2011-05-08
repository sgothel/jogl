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

import javax.media.opengl.GL2ES2;
import com.jogamp.opengl.util.PMVMatrix;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;
import jogamp.graph.curve.opengl.RegionFactory;

/** A Region is the OGL binding of one or more OutlineShapes
 *  Defined by its vertices and generated triangles. The Region
 *  defines the final shape of the OutlineShape(s), which shall produced a shaded 
 *  region on the screen.
 *  
 *  Implementations of the Region shall take care of the OGL 
 *  binding of the depending on its context, profile.
 * 
 * @see RegionFactory, OutlineShape
 */
public abstract class GLRegion extends Region {    
    
    /** Create an ogl {@link GLRegion} defining the list of {@link OutlineShape}.
     * Combining the Shapes into single buffers.
     * @return the resulting Region inclusive the generated region
     */
    public static GLRegion create(OutlineShape[] outlineShapes, int renderModes) {
        final GLRegion region = RegionFactory.create(renderModes);
        
        int numVertices = region.getNumVertices();
        
        for(OutlineShape outlineShape:outlineShapes){
            outlineShape.transformOutlines(OutlineShape.VerticesState.QUADRATIC_NURBS);
    
            ArrayList<Triangle> triangles = outlineShape.triangulate();
            region.addTriangles(triangles);
            
            ArrayList<Vertex> vertices = outlineShape.getVertices();
            for(Vertex vert:vertices){
                vert.setId(numVertices++);
            }
            region.addVertices(vertices);
        }
        
        return region;
    }

    /** 
     * Create an ogl {@link GLRegion} defining this {@link OutlineShape}
     * @return the resulting Region.
     */
    public static GLRegion create(OutlineShape outlineShape, int renderModes) {
        final GLRegion region = RegionFactory.create(renderModes);
        
        outlineShape.transformOutlines(OutlineShape.VerticesState.QUADRATIC_NURBS);
        ArrayList<Triangle> triangles = (ArrayList<Triangle>) outlineShape.triangulate();
        ArrayList<Vertex> vertices = (ArrayList<Vertex>) outlineShape.getVertices();
        region.addVertices(vertices);
        region.addTriangles(triangles);
        return region;
    }        
    
    protected GLRegion(int renderModes) {
        super(renderModes);
    }
    
    /** Updates a graph region by updating the ogl related
     *  objects for use in rendering if {@link #isDirty()}.
     *  <p>Allocates the ogl related data and initializes it the 1st time.<p>  
     *  <p>Called by {@link #draw(GL2ES2, RenderState, int, int, int)}.</p>
     * @param rs TODO
     */
    protected abstract void update(GL2ES2 gl, RenderState rs);
    
    /** Delete and clean the associated OGL
     *  objects
     */
    public abstract void destroy(GL2ES2 gl, RenderState rs);
    
    /** Renders the associated OGL objects specifying
     * current width/hight of window for multi pass rendering
     * of the region.
     * @param matrix current {@link PMVMatrix}.
     * @param vp_width current screen width
     * @param vp_height current screen height
     * @param width texture width for mp rendering
     */
    public final void draw(GL2ES2 gl, RenderState rs, int vp_width, int vp_height, int width) {
        update(gl, rs);
        drawImpl(gl, rs, vp_width, vp_height, width);
    }
    
    protected abstract void drawImpl(GL2ES2 gl, RenderState rs, int vp_width, int vp_height, int width);
}
