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

import jogamp.opengl.Debug;

import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;

/** Abstract Outline shape GL representation
 *  define the method an OutlineShape(s) is
 *  binded rendered.
 *  
 *  @see GLRegion
 */
public abstract class Region {
    
    /** Debug flag for region impl (graph.curve)
     */
    public static final boolean DEBUG = Debug.debug("graph.curve");
    
    public static final boolean DEBUG_INSTANCE = false;

    /** View based Anti-Aliasing, A Two pass region rendering, slower 
     *  and more resource hungry (FBO), but AA is perfect. 
     *  Otherwise the default fast one pass MSAA region rendering is being used. 
     */
    public static final int VBAA_RENDERING_BIT = 1 << 0;

    /** Use non uniform weights [0.0 .. 1.9] for curve region rendering.
     *  Otherwise the default weight 1.0 for uniform curve region rendering is being applied.  
     */
    public static final int VARIABLE_CURVE_WEIGHT_BIT = 1 << 1;

    public static final int TWO_PASS_DEFAULT_TEXTURE_UNIT = 0;

    private final int renderModes;
    private boolean dirty = true;    
    protected int numVertices = 0;    
    protected final AABBox box = new AABBox();
    protected ArrayList<Triangle> triangles = new ArrayList<Triangle>();
    protected ArrayList<Vertex> vertices = new ArrayList<Vertex>();

    public static boolean isVBAA(int renderModes) { 
        return 0 != ( renderModes & Region.VBAA_RENDERING_BIT ); 
    }

    /** Check if render mode capable of non uniform weights
     * @param renderModes bit-field of modes, e.g. {@link Region#VARIABLE_CURVE_WEIGHT_BIT}, 
     * {@link Region#VBAA_RENDERING_BIT} 
     * @return true of capable of non uniform weights
     */
    public static boolean isNonUniformWeight(int renderModes) { 
        return 0 != ( renderModes & Region.VARIABLE_CURVE_WEIGHT_BIT ); 
    }

    protected Region(int regionRenderModes) {
        this.renderModes = regionRenderModes;
    }

    /** Get current Models
     * @return bit-field of render modes 
     */
    public final int getRenderModes() { 
        return renderModes; 
    }

    /** Check if current Region is using VBAA
     * @return true if capable of two pass rendering - VBAA
     */
    public boolean isVBAA() { 
        return Region.isVBAA(renderModes);  
    }

    /** Check if current instance uses non uniform weights 
     * @return true if capable of nonuniform weights
     */
    public boolean isNonUniformWeight() { 
        return Region.isNonUniformWeight(renderModes); 
    }

    /** Get the current number of vertices associated
     * with this region. This number is not necessary equal to 
     * the OGL bound number of vertices.
     * @return vertices count
     */
    public final int getNumVertices(){
        return numVertices;
    }

    /** Adds a {@link Triangle} object to the Region
     * This triangle will be bound to OGL objects 
     * on the next call to {@code update}
     * @param tri a triangle object
     * 
     * @see update(GL2ES2)
     */
    public void addTriangle(Triangle tri) {
        triangles.add(tri);
        setDirty(true);
    }

    /** Adds a list of {@link Triangle} objects to the Region
     * These triangles are to be binded to OGL objects 
     * on the next call to {@code update}
     * @param tris an arraylist of triangle objects
     * 
     * @see update(GL2ES2)
     */
    public void addTriangles(ArrayList<Triangle> tris) {
        triangles.addAll(tris);
        setDirty(true);
    }

    /** Adds a {@link Vertex} object to the Region
     * This vertex will be bound to OGL objects 
     * on the next call to {@code update}
     * @param vert a vertex objects
     * 
     * @see update(GL2ES2)
     */
    public void addVertex(Vertex vert) {
        vertices.add(vert);
        numVertices++;
        setDirty(true);
    }

    /** Adds a list of {@link Vertex} objects to the Region
     * These vertices are to be binded to OGL objects 
     * on the next call to {@code update}
     * @param verts an arraylist of vertex objects
     * 
     * @see update(GL2ES2)
     */
    public void addVertices(ArrayList<Vertex> verts) {
        vertices.addAll(verts);
        numVertices = vertices.size();
        setDirty(true);
    }

    /**
     * @return the AxisAligned bounding box of
     * current region
     */
    public final AABBox getBounds(){
        return box;
    }

    /** Check if this region is dirty. A region is marked dirty
     * when new Vertices, Triangles, and or Lines are added after a 
     * call to update()
     * @return true if region is Dirty, false otherwise
     * 
     * @see update(GL2ES2)
     */
    public final boolean isDirty() {
        return dirty;
    }

    protected final void setDirty(boolean v) {
        dirty = v;
    }
}