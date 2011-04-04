package com.jogamp.graph.curve.opengl;

import java.util.ArrayList;
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
     * @param font {@link Font} to be used
     * @param str text to be rendered 
     * @param position the lower left corner of the string 
     * @param fontSize font size
     * @param texSize texture size for multipass render
     * @throws Exception if TextRenderer not initialized
     */
    public abstract void renderString3D(GL2ES2 gl, Font font,
                                        String str, float[] position, int fontSize, int texSize);

    /**Create the resulting {@link GlyphString} that represents
     * the String wrt to the font.
     * @param font {@link Font} to be used
     * @param size font size
     * @param str {@link String} to be created
     * @param sharpness parameter for Region generation of the resulting GlyphString
     * @return the resulting GlyphString inclusive the generated region
     */
    protected GlyphString createString(GL2ES2 gl, Font font, int size, String str, float sharpness) {
        AffineTransform affineTransform = new AffineTransform(pointFactory);
        
        Path2D[] paths = new Path2D[str.length()];
        ((FontInt)font).getOutline(str, size, affineTransform, paths);
        
        GlyphString glyphString = new GlyphString(pointFactory, font.getName(Font.NAME_UNIQUNAME), str);
        glyphString.createfromFontPath(paths, affineTransform);
        glyphString.generateRegion(gl.getContext(), sharpness, st, renderType);
        
        return glyphString;
    }
    
   public void flushCache() {
       Iterator<GlyphString> iterator = stringCacheMap.values().iterator();
       while(iterator.hasNext()){
           GlyphString glyphString = iterator.next();
           glyphString.destroy();
       }
       stringCacheMap.clear();    
       stringCacheArray.clear();
   }
   
   @Override
   protected void disposeImpl(GL2ES2 gl) {
       flushCache();
   }
   
   public final void setCacheMaxSize(int newSize ) { stringCacheMaxSize = newSize; validateCache(0); }
   public final int getCacheMaxSize() { return stringCacheMaxSize; }
   public final int getCacheSize() { return stringCacheArray.size(); }
   
   protected void validateCache(int space) {
       while ( getCacheSize() + space > getCacheMaxSize() ) {
           String key = stringCacheArray.remove(0);
           stringCacheMap.remove(key);
       }
   }
   
   protected GlyphString getCachedGlyphString(Font font, String str, int fontSize) {
       final String key = font.getName(Font.NAME_UNIQUNAME) + "." + str.hashCode() + "." + fontSize;
       return stringCacheMap.get(key);
   }

   protected void addCachedGlyphString(Font font, String str, int fontSize, GlyphString glyphString) {
       final String key = font.getName(Font.NAME_UNIQUNAME) + "." + str.hashCode() + "." + fontSize;
       validateCache(1);
       stringCacheMap.put(key, glyphString);
       stringCacheArray.add(stringCacheArray.size(), key);
   }

   // Cache is adding at the end of the array
   public static final int DEFAULT_CACHE_SIZE = 32;
   private HashMap<String, GlyphString> stringCacheMap = new HashMap<String, GlyphString>(DEFAULT_CACHE_SIZE);
   private ArrayList<String> stringCacheArray = new ArrayList<String>(DEFAULT_CACHE_SIZE);
   private int stringCacheMaxSize = DEFAULT_CACHE_SIZE; // -1 unlimited, 0 off, >0 limited      
}