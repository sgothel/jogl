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

import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.graph.geom.opengl.SVertex;

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLUniformData;

import jogamp.graph.geom.plane.AffineTransform;
import jogamp.graph.geom.plane.Path2D;
import jogamp.graph.geom.plane.PathIterator;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.RegionFactory;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.opengl.util.glsl.ShaderState;

import java.util.Collection;

/**
 * This class manages a data set of triangles and vertices into the
 * JOGL pipeline.  A GlyphShape is completely formed and considered
 * immutable when it is added to this list.
 */
public class GlyphString
    extends ArrayList<GlyphShape>
{

    private String str = "";
    private String fontname = "";
    private Region region;
    
    private SVertex origin = new SVertex();
    /**
     * Require generateRegion for Region (clear).  Edits not captured
     * by Region are those that drop triangles or vertices.
     */
    private boolean dirty;
    /**
     * Manage the render state sharpness in order to implement direct
     * to region editing
     */
    private float sharpness = 0.5f; 


    /** Create a new GlyphString object
     * @param fontname the name of the font that this String is
     * associated with
     * @param str the string object
     */
    public GlyphString(String fontname, String str){
        super();
        this.fontname = fontname;
        this.str = str;
    }


    public String getString(){
        return str;
    }
    /**
     * @return Require call to {@link #generateRegion} to capture edits
     */
    public boolean isDirty(){
        return this.dirty;
    }
    /** Creates the Curve based Glyphs from a Font 
     * @param pointFactory TODO
     * @param paths a list of FontPath2D objects that define the outline
     * @param affineTransform a global affine transformation applied to the paths.
     */
    public void createfromFontPath(Factory<? extends Vertex> pointFactory, Path2D[] paths, AffineTransform affineTransform) {
        final int numGlyps = paths.length;

        for (int index=0;index<numGlyps;index++){
            if ( null != paths[index]){

                PathIterator iterator = paths[index].iterator(affineTransform);
                GlyphShape glyphShape = new GlyphShape(pointFactory, iterator);
            
                if ( 2 < glyphShape.getNumVertices()) {

                    this.add(glyphShape);
                }
            }
        }
    }
    /**
     * Region data set
     * Vertices get IDs externally -- in Triangulation or Region.
     * @param sharpness 
     * @return Triangulation for glyph string
     */    
    protected ArrayList<Triangle> triangulate(){
        ArrayList<Triangle> triangles = new ArrayList<Triangle>();
        for(GlyphShape glyph: this){
            ArrayList<Triangle> tris = glyph.triangulate(this.sharpness);
            triangles.addAll(tris);
        }
        return triangles;
    }
    /**
     * Region data set
     * @return Vertices require IDs
     */    
    protected ArrayList<Vertex> vertices(){
        if (null != this.region){
            int numVertices = this.region.getNumVertices();
            ArrayList<Vertex> re = new ArrayList<Vertex>();
            for(GlyphShape glyph: this){
                ArrayList<Vertex> gVertices = glyph.getVertices();
                for(Vertex vert: gVertices){
                    vert.setId(numVertices++);
                }
                re.addAll(gVertices);
            }
            return re;
        }
        else
            throw new IllegalStateException();
    }
    
    /** Generate a OGL Region to represent this Object.
     * @param gl the GLContext which the region is defined by.
     * @param rs the rendering context
     * @param type Region rendering type, single or two pass
     */
    public void generateRegion(GL2ES2 gl, RenderState rs, int type){
        /*
         */
        if (null != rs){
            GLUniformData sharpness = rs.getSharpness();
            if (null != sharpness){
                float value = sharpness.floatValue();
                this.dirty = (value != this.sharpness);
                this.sharpness = value;
            }
        }

        if (null == this.region){

            this.region = RegionFactory.create(rs, type);
            this.region.setFlipped(true);
            /*
             */        
            ArrayList<Triangle> tris = this.triangulate();
            this.region.addTriangles(tris);
            /*
             */
            ArrayList<Vertex> vers = this.vertices();
            this.region.addVertices(vers);
            /*
             */
            this.region.update(gl);

            this.dirty = false;
        }
        else if (this.dirty){

            this.region.clear();
            /*
             */        
            ArrayList<Triangle> tris = this.triangulate();
            this.region.addTriangles(tris);
            /*
             */
            ArrayList<Vertex> vers = this.vertices();
            this.region.addVertices(vers);
            /*
             */
            this.region.update(gl);

            this.dirty = false;
        }
        else if (this.region.isDirty()){
            /*
             * clean the region
             */
            this.region.update(gl);
        }
    }
    
    /** Generate a Hashcode for this object 
     * @return a string defining the hashcode
     */
    public String getTextHashCode(){
        return "" + fontname.hashCode() + str.hashCode();
    }

    /** Render the Object based using the associated Region
     *  previously generated.
     */
    public void renderString3D(GL2ES2 gl) {
        region.render(gl, null, 0, 0, 0);
    }
    /** Render the Object based using the associated Region
     *  previously generated.
     */
    public void renderString3D(GL2ES2 gl, RenderState rs, int vp_width, int vp_height, int size) {
        region.render(gl, rs, vp_width, vp_height, size);
    }
    
    /** Get the Origin of this GlyphString
     * @return 
     */
    public Vertex getOrigin() {
        return origin;
    }

    /** Destroy the associated OGL objects
     * @param rs TODO
     */
    public void destroy(GL2ES2 gl, RenderState rs){
        if (null != this.region){
            try {
                this.region.destroy(gl, rs);
            }
            finally {
                this.region = null;
                this.dirty = false;
            }
        }
    }
    
    public AABBox getBounds(){
        if (null != this.region)
            return this.region.getBounds();
        else {
            /*
             * Expected to be used once but not twice before we have a
             * region
             */
            AABBox bbox = new AABBox();
            for (GlyphShape glyph: this){
                bbox.resize(glyph.getBounds());
            }
            return bbox;
        }
    }
    @Override
    public boolean add(GlyphShape gs){

        if (super.add(gs)){

            if (null != this.region && (!this.dirty)){
                /*
                 * Region becomes dirty while GlyphString remains clean
                 */
                this.region.addTriangles(gs.triangulate(this.sharpness));

                ArrayList<Vertex> gVertices = gs.getVertices();
                {
                    int numVertices = this.region.getNumVertices();
                    for(Vertex vert: gVertices){
                        vert.setId(numVertices++);
                    }
                }
                this.region.addVertices(gVertices);
            }
            return true;
        }
        else
            return false;
    }
    /**
     * Insert
     */
    @Override
    public void add(int index, GlyphShape glyphShape) {
        if (null != glyphShape)
            super.add(index,glyphShape);
        else
            throw new IllegalArgumentException();
    }
    /**
     * Replace
     */
    @Override
    public GlyphShape set(int index, GlyphShape gs) {
        if (null != gs){
            this.dirty = (null != this.region); // (old != new)
            return super.set(index,gs);
        }
        else
            throw new IllegalArgumentException();
    }
    @Override
    public boolean addAll(Collection<? extends GlyphShape> c) {
        if (c.isEmpty())
            return false;
        else if (null == this.region || this.dirty){

            return super.addAll(c);
        }
        else {
            boolean mod = false;
            for (GlyphShape gs: c){

                mod = (this.add(gs) || mod);
            }
            return mod;
        }
    }
    @Override
    public boolean addAll(int index, Collection<? extends GlyphShape> c) {
        if (c.isEmpty())
            return false;
        else if (null == this.region || this.dirty){

            return super.addAll(index,c);
        }
        else {

            for (GlyphShape gs: c){

                this.add(index++,gs);
            }
            return true;
        }
    }
    @Override
    public GlyphShape remove(int idx) {
        GlyphShape gs = super.remove(idx);
        if (null != gs)
            this.dirty = true;
        return gs;
    }
    @Override
    public boolean remove(Object o) {
        boolean mod = super.remove(o);
        if (mod)
            this.dirty = true;
        return mod;
    }
    @Override
    public void clear(){
        this.dirty = (null != this.region);
        super.clear();
    }
    public boolean isNotEmpty(){
        return (0 < this.size());
    }
    public GlyphString clone(){
        GlyphString clone = (GlyphString)super.clone();
        clone.region = null;
        clone.dirty = false;
        return clone;
    }
}
