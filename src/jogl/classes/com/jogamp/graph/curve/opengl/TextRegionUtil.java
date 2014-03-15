/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLException;

import jogamp.graph.geom.plane.AffineTransform;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.Font.Glyph;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

/**
 * Text {@link GLRegion} Utility Class
 */
public class TextRegionUtil {

    public final RegionRenderer renderer;

    public TextRegionUtil(final RegionRenderer renderer) {
        this.renderer = renderer;
    }

    public static interface ShapeVisitor {
        /**
         * Visiting the given {@link OutlineShape} with it's corresponding {@link AffineTransform}.
         * @param shape may be used as is, otherwise a copy shall be made if intended to be modified.
         * @param t may be used immediately as is, otherwise a copy shall be made if stored.
         */
        public void visit(final OutlineShape shape, final AffineTransform t);
    }

    /**
     * Visit each {@link Font.Glyph}'s {@link OutlineShape} with the given {@link ShapeVisitor}
     * additionally passing the progressed {@link AffineTransform}.
     * The latter reflects the given font metric, pixelSize and hence character position.
     * @param visitor
     * @param transform
     * @param font the target {@link Font}
     * @param pixelSize Use {@link Font#getPixelSize(float, float)} for resolution correct pixel-size.
     * @param str string text
     */
    public static void processString(final ShapeVisitor visitor, final AffineTransform transform,
                                     final Font font, final float pixelSize, final CharSequence str) {
        final int charCount = str.length();

        // region.setFlipped(true);
        final Font.Metrics metrics = font.getMetrics();
        final float lineHeight = font.getLineHeight(pixelSize);

        final float scale = metrics.getScale(pixelSize);
        final AffineTransform t = new AffineTransform(transform);

        float y = 0;
        float advanceTotal = 0;

        for(int i=0; i< charCount; i++) {
            final char character = str.charAt(i);
            if( '\n' == character ) {
                y -= lineHeight;
                advanceTotal = 0;
            } else if (character == ' ') {
                advanceTotal += font.getAdvanceWidth(Glyph.ID_SPACE, pixelSize);
            } else {
                if(Region.DEBUG_INSTANCE) {
                    System.err.println("XXXXXXXXXXXXXXx char: "+character+", scale: "+scale+"; translate: "+advanceTotal+", "+y);
                }
                t.setTransform(transform); // reset transform
                t.translate(advanceTotal, y);
                t.scale(scale, scale);

                final Font.Glyph glyph = font.getGlyph(character);
                final OutlineShape glyphShape = glyph.getShape();
                if( null == glyphShape ) {
                    continue;
                }
                visitor.visit(glyphShape, t);

                advanceTotal += glyph.getAdvance(pixelSize, true);
            }
        }
    }

    /**
     * Add the string in 3D space w.r.t. the font and pixelSize at the end of the {@link GLRegion}.
     * @param region the {@link GLRegion} sink
     * @param vertexFactory vertex impl factory {@link Factory}
     * @param font the target {@link Font}
     * @param pixelSize Use {@link Font#getPixelSize(float, float)} for resolution correct pixel-size.
     * @param str string text
     */
    public static void addStringToRegion(final GLRegion region, final Factory<? extends Vertex> vertexFactory,
                                         final Font font, final float pixelSize, final CharSequence str) {
        final ShapeVisitor visitor = new ShapeVisitor() {
            public final void visit(final OutlineShape shape, final AffineTransform t) {
                region.addOutlineShape(shape, t);
            } };
        processString(visitor, new AffineTransform(), font, pixelSize, str);
    }

    /**
     * Render the string in 3D space w.r.t. the font and pixelSize
     * using a cached {@link GLRegion} for reuse.
     * <p>
     * Cached {@link GLRegion}s will be destroyed w/ {@link #clear(GL2ES2)} or to free memory.
     * </p>
     * @param gl the current GL state
     * @param font {@link Font} to be used
     * @param pixelSize Use {@link Font#getPixelSize(float, float)} for resolution correct pixel-size.
     * @param str text to be rendered
     * @param sampleCount desired multisampling sample count for msaa-rendering.
     *        The actual used scample-count is written back when msaa-rendering is enabled, otherwise the store is untouched.
     * @throws Exception if TextRenderer not initialized
     */
    public void drawString3D(final GL2ES2 gl,
                             final Font font, final float pixelSize, final CharSequence str,
                             final int[/*1*/] sampleCount) {
        if( !renderer.isInitialized() ) {
            throw new GLException("TextRendererImpl01: not initialized!");
        }
        final int special = 0;
        GLRegion region = getCachedRegion(font, str, pixelSize, special);
        if(null == region) {
            region = GLRegion.create(renderer.getRenderModes());
            addStringToRegion(region, renderer.getRenderState().getVertexFactory(), font, pixelSize, str);
            addCachedRegion(gl, font, str, pixelSize, special, region);
        }
        region.draw(gl, renderer, sampleCount);
    }

    /**
     * Render the string in 3D space w.r.t. the font and pixelSize
     * using a temporary {@link GLRegion}, which will be destroyed afterwards.
     * <p>
     * In case of a multisampling region renderer, i.e. {@link Region#VBAA_RENDERING_BIT}, recreating the {@link GLRegion}
     * is a huge performance impact.
     * In such case better use {@link #drawString3D(GLRegion, RegionRenderer, GL2ES2, Font, float, CharSequence, int[])}
     * instead.
     * </p>
     * @param gl the current GL state
     * @param font {@link Font} to be used
     * @param pixelSize Use {@link Font#getPixelSize(float, float)} for resolution correct pixel-size.
     * @param str text to be rendered
     * @param sampleCount desired multisampling sample count for msaa-rendering.
     *        The actual used scample-count is written back when msaa-rendering is enabled, otherwise the store is untouched.
     * @throws Exception if TextRenderer not initialized
     */
    public static void drawString3D(final RegionRenderer renderer, final GL2ES2 gl,
                                    final Font font, final float pixelSize, final CharSequence str,
                                    final int[/*1*/] sampleCount) {
        if(!renderer.isInitialized()){
            throw new GLException("TextRendererImpl01: not initialized!");
        }
        final GLRegion region = GLRegion.create(renderer.getRenderModes());
        addStringToRegion(region, renderer.getRenderState().getVertexFactory(), font, pixelSize, str);
        region.draw(gl, renderer, sampleCount);
        region.destroy(gl, renderer);
    }

    /**
     * Render the string in 3D space w.r.t. the font and pixelSize
     * using the given {@link GLRegion}, which will {@link GLRegion#clear(GL2ES2, RegionRenderer) cleared} beforehand.
     * @param gl the current GL state
     * @param font {@link Font} to be used
     * @param pixelSize Use {@link Font#getPixelSize(float, float)} for resolution correct pixel-size.
     * @param str text to be rendered
     * @param sampleCount desired multisampling sample count for msaa-rendering.
     *        The actual used scample-count is written back when msaa-rendering is enabled, otherwise the store is untouched.
     * @throws Exception if TextRenderer not initialized
     */
    public static void drawString3D(final GLRegion region, final RegionRenderer renderer, final GL2ES2 gl,
                                    final Font font, final float pixelSize, final CharSequence str,
                                    final int[/*1*/] sampleCount) {
        if(!renderer.isInitialized()){
            throw new GLException("TextRendererImpl01: not initialized!");
        }
        region.clear(gl, renderer);
        addStringToRegion(region, renderer.getRenderState().getVertexFactory(), font, pixelSize, str);
        region.draw(gl, renderer, sampleCount);
    }

   /**
    * Clear all cached {@link GLRegions}.
    */
   public void clear(GL2ES2 gl) {
       // fluchCache(gl) already called
       final Iterator<GLRegion> iterator = stringCacheMap.values().iterator();
       while(iterator.hasNext()){
           final GLRegion region = iterator.next();
           region.destroy(gl, renderer);
       }
       stringCacheMap.clear();
       stringCacheArray.clear();
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
               removeCachedRegion(gl, 0);
           }
       }
   }

   protected final GLRegion getCachedRegion(Font font, CharSequence str, float pixelSize, int special) {
       return stringCacheMap.get(getKey(font, str, pixelSize, special));
   }

   protected final void addCachedRegion(GL2ES2 gl, Font font, CharSequence str, float pixelSize, int special, GLRegion glyphString) {
       if ( 0 != getCacheLimit() ) {
           final String key = getKey(font, str, pixelSize, special);
           final GLRegion oldRegion = stringCacheMap.put(key, glyphString);
           if ( null == oldRegion ) {
               // new entry ..
               validateCache(gl, 1);
               stringCacheArray.add(stringCacheArray.size(), key);
           } /// else overwrite is nop ..
       }
   }

   protected final void removeCachedRegion(GL2ES2 gl, Font font, CharSequence str, int pixelSize, int special) {
       final String key = getKey(font, str, pixelSize, special);
       final GLRegion region = stringCacheMap.remove(key);
       if(null != region) {
           region.destroy(gl, renderer);
       }
       stringCacheArray.remove(key);
   }

   protected final void removeCachedRegion(GL2ES2 gl, int idx) {
       final String key = stringCacheArray.remove(idx);
       if( null != key ) {
           final GLRegion region = stringCacheMap.remove(key);
           if(null != region) {
               region.destroy(gl, renderer);
           }
       }
   }

   protected final String getKey(Font font, CharSequence str, float pixelSize, int special) {
       final StringBuilder sb = new StringBuilder();
       return font.getName(sb, Font.NAME_UNIQUNAME)
              .append(".").append(str.hashCode()).append(".").append(Float.floatToIntBits(pixelSize)).append(special).toString();
   }

   /** Default cache limit, see {@link #setCacheLimit(int)} */
   public static final int DEFAULT_CACHE_LIMIT = 256;

   private final HashMap<String, GLRegion> stringCacheMap = new HashMap<String, GLRegion>(DEFAULT_CACHE_LIMIT);
   private final ArrayList<String> stringCacheArray = new ArrayList<String>(DEFAULT_CACHE_LIMIT);
   private int stringCacheLimit = DEFAULT_CACHE_LIMIT;
}