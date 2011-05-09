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

import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.graph.geom.opengl.SVertex;

import javax.media.opengl.GL2ES2;

import jogamp.graph.curve.opengl.RegionFactory;
import jogamp.graph.font.FontInt;
import jogamp.graph.geom.plane.AffineTransform;
import jogamp.graph.geom.plane.Path2D;
import jogamp.graph.geom.plane.PathIterator;


import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RenderState;

public class GlyphString {
    /** Static font size for all default font OutlineShape generations via {@link #createString(OutlineShape, Factory, Font, String)}.
     * <p>The actual font size shall be accomplished by the GL PMV matrix.</p>
     */
    public static final int STATIC_FONT_SIZE = 10;
    
    private ArrayList<GlyphShape> glyphs = new ArrayList<GlyphShape>();
    private CharSequence str;
    private String fontname;
    private GLRegion region;
    
    private SVertex origin = new SVertex();

    /**
     * <p>Uses {@link #STATIC_FONT_SIZE}.</p>
     * <p>No caching is performed.</p>
     * 
     * @param shape is not null, add all {@link GlyphShape}'s {@link Outline} to this instance.
     * @param vertexFactory
     * @param font
     * @param str
     * @return the created {@link GlyphString} instance
     */
    public static GlyphString createString(OutlineShape shape, Factory<? extends Vertex> vertexFactory, Font font, String str) {
        return createString(shape, vertexFactory, font, STATIC_FONT_SIZE, str); 
    }
    
    /**
     * <p>No caching is performed.</p>
     * 
     * @param shape is not null, add all {@link GlyphShape}'s {@link Outline} to this instance.
     * @param vertexFactory
     * @param font
     * @param size
     * @param str
     * @return the created {@link GlyphString} instance
     */
    public static GlyphString createString(OutlineShape shape, Factory<? extends Vertex> vertexFactory, Font font, int fontSize, String str) {
        AffineTransform affineTransform = new AffineTransform(vertexFactory);
        
        Path2D[] paths = new Path2D[str.length()];
        ((FontInt)font).getPaths(str, fontSize, affineTransform, paths);
        
        GlyphString glyphString = new GlyphString(font.getName(Font.NAME_UNIQUNAME), str);
        glyphString.createfromFontPath(vertexFactory, paths, affineTransform);
        if(null != shape) {
            for(int i=0; i<glyphString.glyphs.size(); i++) {
                shape.addOutlineShape(glyphString.glyphs.get(i).getShape());
            }    
        }
        return glyphString;
    }
    
    /** Create a new GlyphString object
     * @param fontname the name of the font that this String is
     * associated with
     * @param str the string object
     */
    public GlyphString(String fontname, CharSequence str){
        this.fontname = fontname;
        this.str = str;
    }
    
    public void addGlyphShape(GlyphShape glyph){
        glyphs.add(glyph);
    }
    
    public CharSequence getString(){
        return str;
    }

    /** Creates the Curve based Glyphs from a Font 
     * @param vertexFactory vertex impl factory {@link Factory}
     * @param paths a list of FontPath2D objects that define the outline
     * @param affineTransform a global affine transformation applied to the paths.
     */
    public void createfromFontPath(Factory<? extends Vertex> vertexFactory, Path2D[] paths, AffineTransform affineTransform) {
        final int numGlyps = paths.length;
        for (int index=0;index<numGlyps;index++){
            if(paths[index] == null){
                continue;
            }
            PathIterator iterator = paths[index].iterator(affineTransform);
            GlyphShape glyphShape = new GlyphShape(vertexFactory, iterator);
            
            if(glyphShape.getNumVertices() < 3) {
                continue;
            }            
            addGlyphShape(glyphShape);
        }
    }
    
    /** Generate a OGL Region to represent this Object.
     * @param gl the current gl object
     * @param rs the current attached RenderState
     * @param renderModes bit-field of modes, e.g. {@link Region#VARIABLE_CURVE_WEIGHT_BIT}, {@link Region#TWO_PASS_RENDERING_BIT} 
     */
    public GLRegion createRegion(GL2ES2 gl, int renderModes){
        region = RegionFactory.create(renderModes);
        // region.setFlipped(true);
        
        int numVertices = region.getNumVertices();
        
        for(int i=0; i< glyphs.size(); i++) {
            final GlyphShape glyph = glyphs.get(i);
            ArrayList<Triangle> gtris = glyph.triangulate();
            region.addTriangles(gtris);
            
            final ArrayList<Vertex> gVertices = glyph.getVertices();
            for(int j=0; j<gVertices.size(); j++) {
                final Vertex gVert = gVertices.get(j);
                gVert.setId(numVertices++);
                region.addVertex(gVert);
            }
        }
        return region;
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
        region.draw(gl, null, 0, 0, 0);
    }
    /** Render the Object based using the associated Region
     *  previously generated.
     */
    public void renderString3D(GL2ES2 gl, RenderState rs, int vp_width, int vp_height, int size) {
        region.draw(gl, rs, vp_width, vp_height, size);
    }
    
    /** Get the Origin of this GlyphString
     * @return 
     */
    public Vertex getOrigin() {
        return origin;
    }

    /** Destroy the associated OGL objects
     * @param rs the current attached RenderState
     */
    public void destroy(GL2ES2 gl, RenderState rs) {
        if(null != gl && null != rs) {
            region.destroy(gl, rs);
            region = null;
        } else if(null != region) {
            throw new InternalError("destroy called w/o GL context, but has a region");
        }
        glyphs.clear();
    }
    
    public AABBox getBounds(){
        return region.getBounds();
    }
}
