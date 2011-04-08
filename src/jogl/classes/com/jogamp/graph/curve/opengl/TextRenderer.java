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
    public GlyphString createString(GL2ES2 gl, Font font, int size, String str, float sharpness) {
        if(DEBUG) {
            System.err.println("createString: "+getCacheSize()+"/"+getCacheLimit()+" - "+Font.NAME_UNIQUNAME + " - " + str + " - " + size);
        }
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
   
   /**
    * Sets the cache limit for reusing GlyphString's and their Region.
    * Default is {@link #DEFAULT_CACHE_LIMIT}, -1 unlimited, 0 turns cache off, >0 limited
    *  
    * @param newLimit new cache size
    * 
    * @see #DEFAULT_CACHE_LIMIT
    */
   public final void setCacheLimit(int newLimit ) { stringCacheLimit = newLimit; validateCache(0); }
   public final int getCacheLimit() { return stringCacheLimit; }
   
   /** 
    * @return the current utilized cache size, <= {@link #getCacheLimit()}
    */
   public final int getCacheSize() { return stringCacheArray.size(); }
   
   protected final void validateCache(int space) {
       if ( getCacheLimit() > 0 ) {
           while ( getCacheSize() + space > getCacheLimit() ) {
               removeCachedGlyphString(0);
           }
       }
   }
   
   protected final GlyphString getCachedGlyphString(Font font, String str, int fontSize) {
       return stringCacheMap.get(getKey(font, str, fontSize));
   }

   protected final void addCachedGlyphString(Font font, String str, int fontSize, GlyphString glyphString) {
       if ( 0 != getCacheLimit() ) {
           final String key = getKey(font, str, fontSize);
           GlyphString oldGlyphString = stringCacheMap.put(key, glyphString);
           if ( null == oldGlyphString ) {
               // new entry ..
               validateCache(1);
               stringCacheArray.add(stringCacheArray.size(), key);
           } /// else overwrite is nop ..
       }
   }
   
   protected final void removeCachedGlyphString(Font font, String str, int fontSize) {
       final String key = getKey(font, str, fontSize);
       GlyphString glyphString = stringCacheMap.remove(key);
       if(null != glyphString) {
           glyphString.destroy();
       }       
       stringCacheArray.remove(key);
   }

   protected final void removeCachedGlyphString(int idx) {
       final String key = stringCacheArray.remove(idx);
       final GlyphString glyphString = stringCacheMap.remove(key);
       if(null != glyphString) {
           glyphString.destroy();
       }
   }
      
   protected final String getKey(Font font, String str, int fontSize) {
       return font.getName(Font.NAME_UNIQUNAME) + "." + str.hashCode() + "." + fontSize;
   }

   /** Default cache limit, see {@link #setCacheLimit(int)} */
   public static final int DEFAULT_CACHE_LIMIT = 256;
   
   private HashMap<String, GlyphString> stringCacheMap = new HashMap<String, GlyphString>(DEFAULT_CACHE_LIMIT);
   private ArrayList<String> stringCacheArray = new ArrayList<String>(DEFAULT_CACHE_LIMIT);
   private int stringCacheLimit = DEFAULT_CACHE_LIMIT;      
}