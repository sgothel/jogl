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

import jogamp.graph.geom.plane.AffineTransform;
import jogamp.graph.geom.plane.Path2D;
import jogamp.graph.geom.plane.PathIterator;


import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.RegionFactory;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.opengl.util.glsl.ShaderState;

public class GlyphString {
    private ArrayList<GlyphShape> glyphs = new ArrayList<GlyphShape>();
    private String str = "";
    private String fontname = "";
    private Region region;
    
    private SVertex origin = new SVertex();

    /** Create a new GlyphString object
     * @param fontname the name of the font that this String is
     * associated with
     * @param str the string object
     */
    public GlyphString(String fontname, String str){
        this.fontname = fontname;
        this.str = str;
    }
    
    public void addGlyphShape(GlyphShape glyph){
        glyphs.add(glyph);
    }
    public String getString(){
        return str;
    }

    /** Creates the Curve based Glyphs from a Font 
     * @param pointFactory TODO
     * @param paths a list of FontPath2D objects that define the outline
     * @param affineTransform a global affine transformation applied to the paths.
     */
    public void createfromFontPath(Factory<? extends Vertex> pointFactory, Path2D[] paths, AffineTransform affineTransform) {
        final int numGlyps = paths.length;
        for (int index=0;index<numGlyps;index++){
            if(paths[index] == null){
                continue;
            }
            PathIterator iterator = paths[index].iterator(affineTransform);
            GlyphShape glyphShape = new GlyphShape(pointFactory, iterator);
            
            if(glyphShape.getNumVertices() < 3) {
                continue;
            }            
            addGlyphShape(glyphShape);
        }
    }
    
    private ArrayList<Triangle> initializeTriangles(float sharpness){
        ArrayList<Triangle> triangles = new ArrayList<Triangle>();
        for(GlyphShape glyph:glyphs){
            ArrayList<Triangle> tris = glyph.triangulate(sharpness);
            triangles.addAll(tris);
        }
        return triangles;
    }
    
    /** Generate a OGL Region to represent this Object.
     * @param context the GLContext which the region is defined by.
     * @param shaprness the curvature sharpness of the object.
     * @param st shader state
     */
    public void generateRegion(GL2ES2 gl, RenderState rs, int type){
        region = RegionFactory.create(rs, type);
        region.setFlipped(true);
        
        ArrayList<Triangle> tris = initializeTriangles(rs.getSharpness().floatValue());
        region.addTriangles(tris);
        
        int numVertices = region.getNumVertices();
        for(GlyphShape glyph:glyphs){
            ArrayList<Vertex> gVertices = glyph.getVertices();
            for(Vertex vert:gVertices){
                vert.setId(numVertices++);
            }
            region.addVertices(gVertices);
        }
        
        /** initialize the region */
        region.update(gl);
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
        region.destroy(gl, rs);
    }
    
    public AABBox getBounds(){
        return region.getBounds();
    }
}
