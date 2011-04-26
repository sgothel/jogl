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
import java.util.HashMap;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;

import jogamp.graph.curve.text.GlyphString;
import jogamp.graph.font.FontInt;
import jogamp.graph.geom.plane.AffineTransform;
import jogamp.graph.geom.plane.Path2D;

import com.jogamp.graph.font.Font;

public abstract class TextRenderer extends Renderer {
    /** 
     * Create a Hardware accelerated Text Renderer.
     * @param rs the used {@link RenderState} 
     * @param renderType either {@link com.jogamp.graph.curve.Region#SINGLE_PASS} or {@link com.jogamp.graph.curve.Region#TWO_PASS}
     */
    public static TextRenderer create(RenderState rs, int type) {
        return new jogamp.graph.curve.opengl.TextRendererImpl01(rs, type);
    }
    
    protected TextRenderer(RenderState rs, int type) {
        super(rs, type);
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
     * @return the resulting GlyphString inclusive the generated region
     */
    public GlyphString createString(GL2ES2 gl, Font font, int size, String str) {
        if(DEBUG_INSTANCE) {
            System.err.println("createString: "+getCacheSize()+"/"+getCacheLimit()+" - "+Font.NAME_UNIQUNAME + " - " + str + " - " + size);
        }
        AffineTransform affineTransform = new AffineTransform(rs.getPointFactory());
        
        Path2D[] paths = new Path2D[str.length()];
        ((FontInt)font).getOutline(str, size, affineTransform, paths);
        
        GlyphString glyphString = new GlyphString(font.getName(Font.NAME_UNIQUNAME), str);
        glyphString.createfromFontPath(rs.getPointFactory(), paths, affineTransform);
        glyphString.generateRegion(gl, rs, renderType);
        
        return glyphString;
    }
    
   public void flushCache(GL2ES2 gl) {
       Iterator<GlyphString> iterator = stringCacheMap.values().iterator();
       while(iterator.hasNext()){
           GlyphString glyphString = iterator.next();
           glyphString.destroy(gl, rs);
       }
       stringCacheMap.clear();    
       stringCacheArray.clear();
   }
   
   @Override
   protected void disposeImpl(GL2ES2 gl) {
       // fluchCache(gl) already called
   }
   
   /**
    * <p>Sets the cache limit for reusing GlyphString's and their Region.
    * Default is {@link #DEFAULT_CACHE_LIMIT}, -1 unlimited, 0 turns cache off, >0 limited </p>
    * 
    * <p>The cache will be validate when the next string rendering happens.</p>
    *  
    * @param newLimit new cache size
    * 
    * @see #DEFAULT_CACHE_LIMIT
    */
   public final void setCacheLimit(int newLimit ) { stringCacheLimit = newLimit; }
   
   /**
    * Sets the cache limit, see {@link #setCacheLimit(int)} and validates the cache.
    * 
    * @see #setCacheLimit(int)
    * 
    * @param gl current GL used to remove cached objects if required
    * @param newLimit new cache size
    */
   public final void setCacheLimit(GL2ES2 gl, int newLimit ) { stringCacheLimit = newLimit; validateCache(gl, 0); }
   
   /**
    * @return the current cache limit
    */
   public final int getCacheLimit() { return stringCacheLimit; }
   
   /** 
    * @return the current utilized cache size, <= {@link #getCacheLimit()}
    */
   public final int getCacheSize() { return stringCacheArray.size(); }
   
   protected final void validateCache(GL2ES2 gl, int space) {
       if ( getCacheLimit() > 0 ) {
           while ( getCacheSize() + space > getCacheLimit() ) {
               removeCachedGlyphString(gl, 0);
           }
       }
   }
   
   protected final GlyphString getCachedGlyphString(Font font, String str, int fontSize) {
       return stringCacheMap.get(getKey(font, str, fontSize));
   }

   protected final void addCachedGlyphString(GL2ES2 gl, Font font, String str, int fontSize, GlyphString glyphString) {
       if ( 0 != getCacheLimit() ) {
           final String key = getKey(font, str, fontSize);
           GlyphString oldGlyphString = stringCacheMap.put(key, glyphString);
           if ( null == oldGlyphString ) {
               // new entry ..
               validateCache(gl, 1);
               stringCacheArray.add(stringCacheArray.size(), key);
           } /// else overwrite is nop ..
       }
   }
   
   protected final void removeCachedGlyphString(GL2ES2 gl, Font font, String str, int fontSize) {
       final String key = getKey(font, str, fontSize);
       GlyphString glyphString = stringCacheMap.remove(key);
       if(null != glyphString) {
           glyphString.destroy(gl, rs);
       }       
       stringCacheArray.remove(key);
   }

   protected final void removeCachedGlyphString(GL2ES2 gl, int idx) {
       final String key = stringCacheArray.remove(idx);
       final GlyphString glyphString = stringCacheMap.remove(key);
       if(null != glyphString) {
           glyphString.destroy(gl, rs);
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