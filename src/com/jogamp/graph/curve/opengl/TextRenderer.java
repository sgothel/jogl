package com.jogamp.graph.curve.opengl;

import java.util.HashMap;
import java.util.Iterator;

import javax.media.opengl.GL2ES2;

import jogamp.graph.curve.text.GlyphString;
import jogamp.graph.font.FontInt;
import jogamp.graph.geom.plane.AffineTransform;
import jogamp.graph.geom.plane.Path2D;

import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.Vertex;

public abstract class TextRenderer extends Renderer {
    
    /** 
     * Create a Hardware accelerated Text Renderer.
     * @param factory optional Point.Factory for Vertex construction. Default is Vertex.Factory.
     */
    public static TextRenderer create(Vertex.Factory<? extends Vertex> factory, int type) {
        return new jogamp.graph.curve.opengl.TextRendererImpl01(factory, type);
    }
    
    protected TextRenderer(Vertex.Factory<? extends Vertex> factory, int type) {
        super(factory, type);
    }

    /** Render the String in 3D space wrt to the font provided at the position provided
     * the outlines will be generated, if not yet generated
     * @param gl the current GL state
     * @param font font to be used
     * @param str text to be rendered 
     * @param position the lower left corner of the string 
     * @param fontSize font size
     * @param texSize texture size for multipass render
     * @throws Exception if TextRenderer not initialized
     */
    public abstract void renderString3D(GL2ES2 gl, Font font,
                                        String str, float[] position, int fontSize, int texSize);

    protected HashMap<String, GlyphString> strings = new HashMap<String, GlyphString>();

    /**
     * 
     * @param font
     * @param size
     * @param str
     * @param sharpness parameter for Region generation of the resulting GlyphString
     * @return the resulting GlyphString inclusive the generated region
     */
    protected GlyphString createString(GL2ES2 gl, Font font, int size, String str, float sharpness) {
        AffineTransform affineTransform = new AffineTransform(pointFactory);
        
        Path2D[] paths = new Path2D[str.length()];
        ((FontInt)font).getOutline(str, size, affineTransform, paths);
        
        GlyphString glyphString = new GlyphString(pointFactory, font.getName(), str);
        glyphString.createfromFontPath(paths, affineTransform);
        glyphString.generateRegion(gl.getContext(), sharpness, st, regionType);
        
        return glyphString;
    }
        
   protected static String getTextHashCode(Font font, String str, int fontSize) {
       // FIXME: use integer hash code
       return font.getName() + "." + str.hashCode() + "." + fontSize;
   }
   
   public void flushCash() {
       Iterator<GlyphString> iterator = strings.values().iterator();
       while(iterator.hasNext()){
           GlyphString glyphString = iterator.next();
           glyphString.destroy();
       }
       strings.clear();    
   }       
}