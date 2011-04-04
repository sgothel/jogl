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
    
    /** Create a Hardware accelerated Curve Region Renderer
     */
    public static RegionRenderer create(Vertex.Factory<? extends Vertex> factory, int type) {
        return new jogamp.graph.curve.opengl.RegionRendererImpl01(factory, type);
    }
    
    public RegionRenderer(Vertex.Factory<? extends Vertex> factory, int type) {
        super(factory, type);
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

    public void flushCache() {
        Iterator<Region> iterator = regions.values().iterator();
        while(iterator.hasNext()){
            Region region = iterator.next();
            region.destroy();
        }
        regions.clear();
    }       

    @Override
    protected void disposeImpl(GL2ES2 gl) {
        flushCache();
    }
    
    /** Create an ogl {@link Region} defining this {@link OutlineShape}
     * @param sharpness parameter for Region generation
     * @return the resulting Region.
     */
    protected Region createRegion(GL2ES2 gl, OutlineShape outlineShape, float sharpness) {
        Region region = RegionFactory.create(gl.getContext(), st, renderType);
        
        outlineShape.transformOutlines(OutlineShape.QUADRATIC_NURBS);
        
        ArrayList<Triangle> triangles = (ArrayList<Triangle>) outlineShape.triangulate(sharpness);
        ArrayList<Vertex> vertices = (ArrayList<Vertex>) outlineShape.getVertices();
        region.addVertices(vertices);
        region.addTriangles(triangles);
        
        region.update();
        return region;
    }
    
    /** Create an ogl {@link Region} defining the list of {@link OutlineShape}.
     * Combining the Shapes into single buffers.
     * @param sharpness parameter for Region generation
     * @return the resulting Region inclusive the generated region
     */
    protected Region createRegion(GL2ES2 gl, OutlineShape[] outlineShapes, float sharpness) {
        Region region = RegionFactory.create(gl.getContext(), st, renderType);
        
        int numVertices = region.getNumVertices();
        
        for(OutlineShape outlineShape:outlineShapes){
            outlineShape.transformOutlines(OutlineShape.QUADRATIC_NURBS);

            ArrayList<Triangle> triangles = outlineShape.triangulate(sharpness);
            region.addTriangles(triangles);
            
            ArrayList<Vertex> vertices = outlineShape.getVertices();
            for(Vertex vert:vertices){
                vert.setId(numVertices++);
            }
            region.addVertices(vertices);
        }
        
        region.update();
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