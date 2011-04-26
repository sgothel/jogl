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

import javax.media.opengl.GL2ES2;

import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.AABBox;
import jogamp.opengl.Debug;

import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.util.PMVMatrix;

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
public interface Region {
    public static final boolean DEBUG = Debug.debug("graph.curve");
    public static final boolean DEBUG_INSTANCE = false;
    
    /** single pass rendering, fast, but AA might not be perfect */
    public static int SINGLE_PASS = 1;
    
    /** two pass rendering, slower and more resource hungry (FBO), but AA is perfect */
    public static int TWO_PASS    = 2;
    public static int TWO_PASS_DEFAULT_TEXTURE_UNIT = 0;
    
    /** Updates a graph region by updating the ogl related
     *  objects for use in rendering. if called for the first time
     *  it initialize the objects. 
     */
    public void update(GL2ES2 gl);
    
    /** Renders the associated OGL objects specifying
     * current width/hight of window for multi pass rendering
     * of the region.
     * @param matrix current {@link PMVMatrix}.
     * @param vp_width current screen width
     * @param vp_height current screen height
     * @param width texture width for mp rendering
     * 
     * @see update()
     */
    public void render(GL2ES2 gl, RenderState rs, int vp_width, int vp_height, int width);
    
    /** Adds a list of {@link Triangle} objects to the Region
     * These triangles are to be binded to OGL objects 
     * on the next call to {@code update}
     * @param tris an arraylist of triangle objects
     * 
     * @see update()
     */
    public void addTriangles(ArrayList<Triangle> tris);
    
    /** Get the current number of vertices associated
     * with this region. This number is not necessary equal to 
     * the OGL binded number of vertices.
     * @return vertices count
     * 
     * @see isDirty()
     */
    public int getNumVertices();
    
    /** Adds a list of {@link Vertex} objects to the Region
     * These vertices are to be binded to OGL objects 
     * on the next call to {@code update}
     * @param verts an arraylist of vertex objects
     * 
     * @see update()
     */
    public void addVertices(ArrayList<Vertex> verts);
    
    /** Check if this region is dirty. A region is marked dirty
     * when new Vertices, Triangles, and or Lines are added after a 
     * call to update()
     * @return true if region is Dirty, false otherwise
     * 
     * @see update();
     */
    public boolean isDirty();
    
    /** Delete and clean the associated OGL
     *  objects
     */
    public void destroy(GL2ES2 gl, RenderState rs);
    
    public AABBox getBounds(); 
    
    public boolean isFlipped();
    
    /** Set if the y coordinate of the region should be flipped
     *  {@code y=-y} used mainly for fonts since they use opposite vertex
     *  as origion
     * @param flipped flag if the coordinate is flipped defaults to false.
     */
    public void setFlipped(boolean flipped);
}
