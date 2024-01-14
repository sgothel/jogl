/**
 * Copyright 2014-2024 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLException;
import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.Font.Glyph;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.plane.AffineTransform;

/**
 * Text Type Rendering Utility Class adding the {@link Font.Glyph}s {@link OutlineShape} to a {@link GLRegion}.
 * <p>
 * {@link OutlineShape}s are all produced in font em-size [0..1].
 * </p>
 */
public class TextRegionUtil {

    public final int renderModes;

    public TextRegionUtil(final int renderModes) {
        this.renderModes = renderModes;
    }

    public static int getCharCount(final String s, final char c) {
        final int sz = s.length();
        int count = 0;
        for(int i=0; i<sz; i++) {
            if( s.charAt(i) == c ) {
                count++;
            }
        }
        return count;
    }

    /**
     * Add the string in 3D space w.r.t. the font in font em-size [0..1] at the end of the {@link GLRegion}
     * while passing the progressed {@link AffineTransform}.
     * <p>
     * The shapes added to the GLRegion are in font em-size [0..1], but can be adjusted with the given transform, progressed and passed to the visitor.
     * </p>
     * <p>
     * Origin of rendered text is 0/0 at bottom left.
     * </p>
     * <p>
     * The region buffer's size is grown by pre-calculating required size via {@link #countStringRegion(Font, CharSequence, int[])}.
     * </p>
     * @param region the {@link GLRegion} sink
     * @param font the target {@link Font}
     * @param transform optional given transform
     * @param str string text
     * @param rgbaColor if {@link Region#hasColorChannel()} RGBA color must be passed, otherwise value is ignored.
     * @return the bounding box of the given string by taking each glyph's font em-sized [0..1] OutlineShape into account.
     */
    public static AABBox addStringToRegion(final Region region, final Font font, final AffineTransform transform,
                                           final CharSequence str, final Vec4f rgbaColor) {
        return addStringToRegion(true /* preGrowRegion */, region, font, transform, str, rgbaColor, new AffineTransform(), new AffineTransform());
    }

    /**
     * Add the string in 3D space w.r.t. the font in font em-size [0..1] at the end of the {@link GLRegion}
     * while passing the progressed {@link AffineTransform}.
     * <p>
     * The shapes added to the GLRegion are in font em-size [0..1], but can be adjusted with the given transform, progressed and passed to the visitor.
     * </p>
     * <p>
     * Origin of rendered text is 0/0 at bottom left.
     * </p>
     * <p>
     * The region buffer's size is grown by pre-calculating required size via {@link #countStringRegion(Font, CharSequence, int[])}.
     * </p>
     * @param region the {@link GLRegion} sink
     * @param font the target {@link Font}
     * @param transform optional given transform
     * @param str string text
     * @param rgbaColor if {@link Region#hasColorChannel()} RGBA color must be passed, otherwise value is ignored.
     * @param temp1 temporary AffineTransform storage, mandatory
     * @param temp2 temporary AffineTransform storage, mandatory
     * @return the bounding box of the given string by taking each glyph's font em-sized [0..1] OutlineShape into account.
     */
    public static AABBox addStringToRegion(final Region region, final Font font, final AffineTransform transform,
                                           final CharSequence str, final Vec4f rgbaColor,
                                           final AffineTransform temp1, final AffineTransform temp2) {
        return addStringToRegion(true /* preGrowRegion */, region, font, transform, str, rgbaColor, temp1, temp2);
    }

    /**
     * Add the string in 3D space w.r.t. the font in font em-size [0..1] at the end of the {@link GLRegion}
     * while passing the progressed {@link AffineTransform}.
     * <p>
     * The shapes added to the GLRegion are in font em-size [0..1], but can be adjusted with the given transform, progressed and passed to the visitor.
     * </p>
     * <p>
     * Origin of rendered text is 0/0 at bottom left.
     * </p>
     * <p>
     * Depending on `preGrowRegion`, the region buffer's size is grown by pre-calculating required size via {@link #countStringRegion(Font, CharSequence, int[])}.
     * </p>
     * @param preGrowRegion if true, utilizes {@link #countStringRegion(Font, CharSequence, int[])} to pre-calc required buffer size, otherwise not.
     * @param region the {@link GLRegion} sink
     * @param font the target {@link Font}
     * @param transform optional given transform
     * @param str string text
     * @param rgbaColor if {@link Region#hasColorChannel()} RGBA color must be passed, otherwise value is ignored.
     * @param temp1 temporary AffineTransform storage, mandatory
     * @param temp2 temporary AffineTransform storage, mandatory
     * @return the bounding box of the given string by taking each glyph's font em-sized [0..1] OutlineShape into account.
     */
    public static AABBox addStringToRegion(final boolean preGrowRegion, final Region region, final Font font, final AffineTransform transform,
                                           final CharSequence str, final Vec4f rgbaColor,
                                           final AffineTransform temp1, final AffineTransform temp2) {
        final Font.GlyphVisitor visitor = new Font.GlyphVisitor() {
            @Override
            public void visit(final Glyph glyph, final AffineTransform t) {
                if( !glyph.isNonContour() ) {
                    region.addOutlineShape(glyph.getShape(), t, rgbaColor);
                }
            }
        };
        if( preGrowRegion ) {
            final int[] vertIndCount = countStringRegion(font, str, new int[2]);
            region.growBuffer(vertIndCount[0], vertIndCount[1]);
        }
        return font.processString(visitor, transform, str, temp1, temp2);
    }

    /**
     * Count required number of vertices and indices adding to given int[2] `vertIndexCount` array.
     * <p>
     * The region's buffer can be either set using {@link Region#setBufferCapacity(int, int)} or grown using {@link Region#growBuffer(int, int)}.
     * </p>
     * @param font the target {@link Font}
     * @param str string text
     * @param vertIndexCount the int[2] storage where the counted vertices and indices are added, vertices at [0] and indices at [1]
     * @return the given int[2] storage for chaining
     * @see Region#setBufferCapacity(int, int)
     * @see Region#growBuffer(int, int)
     * @see #drawString3D(GL2ES2, GLRegion, RegionRenderer, Font, CharSequence, Vec4f, int[], AffineTransform, AffineTransform)
     */
    public static int[] countStringRegion(final Font font, final CharSequence str, final int[/*2*/] vertIndexCount) {
        final Font.GlyphVisitor2 visitor = new Font.GlyphVisitor2() {
            @Override
            public final void visit(final Font.Glyph glyph) {
                if( !glyph.isNonContour() ) {
                    Region.countOutlineShape(glyph.getShape(), vertIndexCount);
                }
            } };
        font.processString(visitor, str);
        return vertIndexCount;
    }

    /**
     * Render the string in 3D space w.r.t. the font int font em-size [0..1] at the end of an internally cached {@link GLRegion}.
     * <p>
     * The shapes added to the GLRegion are in font em-size [0..1].
     * </p>
     * <p>
     * Origin of rendered text is 0/0 at bottom left.
     * </p>
     * <p>
     * Cached {@link GLRegion}s will be destroyed w/ {@link #clear(GL2ES2)} or to free memory.
     * </p>
     * <p>
     * The region's buffer size is pre-calculated via {@link GLRegion#create(com.jogamp.opengl.GLProfile, int, com.jogamp.opengl.util.texture.TextureSequence, Font, CharSequence)}
     * </p>
     * @param gl the current GL state
     * @param renderer TODO
     * @param font {@link Font} to be used
     * @param str text to be rendered
     * @param rgbaColor used to fill the {@link Region#hasColorChannel() region's color-channel} if used
     *                  and set {@link RegionRenderer#setColorStatic(Vec4f) renderer's static-color} to white.
     *                  Otherwise used to set the {@link RegionRenderer#setColorStatic(Vec4f) renderer's static-color} only, if not {@code null}.
     * @param sampleCount desired multisampling sample count for msaa-rendering.
     *        The actual used scample-count is written back when msaa-rendering is enabled, otherwise the store is untouched.
     * @return the bounding box of the given string from the produced and rendered GLRegion
     * @throws Exception if TextRenderer not initialized
     */
    public AABBox drawString3D(final GL2ES2 gl,
                               final RegionRenderer renderer, final Font font, final CharSequence str,
                               final Vec4f rgbaColor, final int[/*1*/] sampleCount) {
        if( !renderer.isInitialized() ) {
            throw new GLException("TextRendererImpl01: not initialized!");
        }
        GLRegion region = getCachedRegion(font, str);
        AABBox res;
        if(null == region) {
            region = GLRegion.create(gl.getGLProfile(), renderModes, null, font, str);
            res = addStringToRegion(false /* preGrowRegion */, region, font, null, str, rgbaColor, tempT1, tempT2);
            addCachedRegion(gl, font, str, region);
        } else {
            res = new AABBox();
            res.copy(region.getBounds());
        }
        if( !region.hasColorChannel() ) {
            if( null != rgbaColor ) {
                renderer.setColorStatic(rgbaColor);
            }
        } else {
            renderer.setColorStatic(1, 1, 1, 1);
        }
        region.draw(gl, renderer, Region.MAX_AA_QUALITY, sampleCount);
        return res;
    }

    /**
     * Try using {@link #drawString3D(GL2ES2, int, RegionRenderer, Font, CharSequence, Vec4f, int[], AffineTransform, AffineTransform)} to reuse {@link AffineTransform} instances.
     * <p>
     * The region's buffer size is pre-calculated via {@link GLRegion#create(com.jogamp.opengl.GLProfile, int, com.jogamp.opengl.util.texture.TextureSequence, Font, CharSequence)}
     * </p>
     */
    public static AABBox drawString3D(final GL2ES2 gl, final int renderModes,
                                      final RegionRenderer renderer, final Font font, final CharSequence str,
                                      final Vec4f rgbaColor, final int[/*1*/] sampleCount) {
        return drawString3D(gl, renderModes, renderer, font, str, rgbaColor, sampleCount, new AffineTransform(), new AffineTransform());
    }

    /**
     * Render the string in 3D space w.r.t. the font in font em-size [0..1] at the end of an internally temporary {@link GLRegion}.
     * <p>
     * The shapes added to the GLRegion are in font em-size [0..1].
     * </p>
     * <p>
     * Origin of rendered text is 0/0 at bottom left.
     * </p>
     * <p>
     * The region's buffer size is pre-calculated via {@link GLRegion#create(com.jogamp.opengl.GLProfile, int, com.jogamp.opengl.util.texture.TextureSequence, Font, CharSequence)}
     * </p>
     * <p>
     * In case of a multisampling region renderer, i.e. {@link Region#VBAA_RENDERING_BIT}, recreating the {@link GLRegion}
     * is a huge performance impact.
     * In such case better use {@link #drawString3D(GL2ES2, GLRegion, RegionRenderer, Font, CharSequence, Vec4f, int[], AffineTransform, AffineTransform)}
     * instead.
     * </p>
     * @param gl the current GL state
     * @param renderModes TODO
     * @param font {@link Font} to be used
     * @param str text to be rendered
     * @param rgbaColor used to fill the {@link Region#hasColorChannel() region's color-channel} if used
     *                  and set {@link RegionRenderer#setColorStatic(Vec4f) renderer's static-color} to white.
     *                  Otherwise used to set the {@link RegionRenderer#setColorStatic(Vec4f) renderer's static-color} only, if not {@code null}.
     * @param sampleCount desired multisampling sample count for msaa-rendering.
     *        The actual used scample-count is written back when msaa-rendering is enabled, otherwise the store is untouched.
     * @param tmp1 temp {@link AffineTransform} to be reused
     * @param tmp2 temp {@link AffineTransform} to be reused
     * @throws Exception if TextRenderer not initialized
     * @return the bounding box of the given string from the produced and rendered GLRegion
     */
    public static AABBox drawString3D(final GL2ES2 gl, final int renderModes,
                                      final RegionRenderer renderer, final Font font, final CharSequence str,
                                      final Vec4f rgbaColor, final int[/*1*/] sampleCount, final AffineTransform tmp1, final AffineTransform tmp2) {
        if(!renderer.isInitialized()){
            throw new GLException("TextRendererImpl01: not initialized!");
        }
        final GLRegion region = GLRegion.create(gl.getGLProfile(), renderModes, null, font, str);
        final AABBox res = addStringToRegion(false /* preGrowRegion */, region, font, null, str, rgbaColor, tmp1, tmp2);
        if( !region.hasColorChannel() ) {
            if( null != rgbaColor ) {
                renderer.setColorStatic(rgbaColor);
            }
        } else {
            renderer.setColorStatic(1, 1, 1, 1);
        }
        region.draw(gl, renderer, Region.MAX_AA_QUALITY, sampleCount);
        region.destroy(gl);
        return res;
    }

    /**
     * Try using {@link #drawString3D(GL2ES2, GLRegion, RegionRenderer, Font, CharSequence, Vec4f, int[], AffineTransform, AffineTransform)} to reuse {@link AffineTransform} instances.
     * <p>
     * The region buffer's size is grown by pre-calculating required size via {@link #countStringRegion(Font, CharSequence, int[])}.
     * </p>
     */
    public static AABBox drawString3D(final GL2ES2 gl, final GLRegion region, final RegionRenderer renderer,
                                      final Font font, final CharSequence str, final Vec4f rgbaColor, final int[/*1*/] sampleCount) {
        return drawString3D(gl, region, renderer, font, str, rgbaColor, sampleCount, new AffineTransform(), new AffineTransform());
    }

    /**
     * Render the string in 3D space w.r.t. the font in font em-size [0..1] at the end of the given {@link GLRegion}.
     * <p>
     * User might want to {@link GLRegion#clear(GL2ES2)} the region before calling this method.
     * </p>
     * <p>
     * The shapes added to the GLRegion are in font em-size [0..1].
     * </p>
     * <p>
     * Origin of rendered text is 0/0 at bottom left.
     * </p>
     * <p>
     * The region buffer's size is grown by pre-calculating required size via {@link #countStringRegion(Font, CharSequence, int[])}.
     * </p>
     * @param gl the current GL state
     * @param region
     * @param renderer
     * @param font {@link Font} to be used
     * @param str text to be rendered
     * @param rgbaColor used to fill the {@link Region#hasColorChannel() region's color-channel} if used
     *                  and set {@link RegionRenderer#setColorStatic(Vec4f) renderer's static-color} to white.
     *                  Otherwise used to set the {@link RegionRenderer#setColorStatic(Vec4f) renderer's static-color} only, if not {@code null}.
     * @param sampleCount desired multisampling sample count for msaa-rendering.
     *        The actual used scample-count is written back when msaa-rendering is enabled, otherwise the store is untouched.
     * @param tmp1 temp {@link AffineTransform} to be reused
     * @param tmp2 temp {@link AffineTransform} to be reused
     * @return the bounding box of the given string from the produced and rendered GLRegion
     * @throws Exception if TextRenderer not initialized
     */
    public static AABBox drawString3D(final GL2ES2 gl, final GLRegion region, final RegionRenderer renderer,
                                      final Font font, final CharSequence str, final Vec4f rgbaColor,
                                      final int[/*1*/] sampleCount, final AffineTransform tmp1, final AffineTransform tmp2) {
        if(!renderer.isInitialized()){
            throw new GLException("TextRendererImpl01: not initialized!");
        }
        final AABBox res = addStringToRegion(true /* preGrowRegion */, region, font, null, str, rgbaColor, tmp1, tmp2);
        if( !region.hasColorChannel() ) {
            if( null != rgbaColor ) {
                renderer.setColorStatic(rgbaColor);
            }
        } else {
            renderer.setColorStatic(1, 1, 1, 1);
        }
        region.draw(gl, renderer, Region.MAX_AA_QUALITY, sampleCount);
        return res;
    }

   /**
    * Clear all cached {@link GLRegions} and mapped values.
    */
   public void clear(final GL2ES2 gl) {
       // fluchCache(gl) already called
       final Iterator<GLRegion> iterator = stringCacheMap.values().iterator();
       while(iterator.hasNext()){
           final GLRegion region = iterator.next();
           region.destroy(gl);
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
   public final void setCacheLimit(final int newLimit ) { stringCacheLimit = newLimit; }

   /**
    * Sets the cache limit, see {@link #setCacheLimit(int)} and validates the cache.
    *
    * @see #setCacheLimit(int)
    *
    * @param gl current GL used to remove cached objects if required
    * @param newLimit new cache size
    */
   public final void setCacheLimit(final GL2ES2 gl, final int newLimit ) { stringCacheLimit = newLimit; validateCache(gl, 0); }

   /**
    * @return the current cache limit
    */
   public final int getCacheLimit() { return stringCacheLimit; }

   /**
    * @return the current utilized cache size, <= {@link #getCacheLimit()}
    */
   public final int getCacheSize() { return stringCacheArray.size(); }

   private final void validateCache(final GL2ES2 gl, final int space) {
       if ( getCacheLimit() > 0 ) {
           while ( getCacheSize() + space > getCacheLimit() ) {
               removeCachedRegion(gl, 0);
           }
       }
   }

   private final GLRegion getCachedRegion(final Font font, final CharSequence str) {
       return stringCacheMap.get(new Key(font, str));
   }

   private final void addCachedRegion(final GL2ES2 gl, final Font font, final CharSequence str, final GLRegion glyphString) {
       if ( 0 != getCacheLimit() ) {
           final Key key = new Key(font, str);
           final GLRegion oldRegion = stringCacheMap.put(key, glyphString);
           if ( null == oldRegion ) {
               // new entry ..
               validateCache(gl, 1);
               stringCacheArray.add(stringCacheArray.size(), key);
           } /// else overwrite is nop ..
       }
   }

   private final void removeCachedRegion(final GL2ES2 gl, final Font font, final CharSequence str) {
       final Key key = new Key(font, str);
       final GLRegion region = stringCacheMap.remove(key);
       if(null != region) {
           region.destroy(gl);
       }
       stringCacheArray.remove(key);
   }

   private final void removeCachedRegion(final GL2ES2 gl, final int idx) {
       final Key key = stringCacheArray.remove(idx);
       if( null != key ) {
           final GLRegion region = stringCacheMap.remove(key);
           if(null != region) {
               region.destroy(gl);
           }
       }
   }

   private class Key {
       private final String fontName;
       private final CharSequence text;
       public final int hash;

       public Key(final Font font, final CharSequence text) {
           this.fontName = font.getName(Font.NAME_UNIQUNAME);
           this.text = text;

           // 31 * x == (x << 5) - x
           final int lhash = 31 + fontName.hashCode();
           this.hash = ((lhash << 5) - lhash) + text.hashCode();
       }

       @Override
       public final int hashCode() { return hash; }

       @Override
       public final boolean equals(final Object o) {
           if( this == o ) { return true; }
           if( o instanceof Key ) {
               final Key ok = (Key)o;
               return ok.fontName.equals(fontName) && ok.text.equals(text);
           }
           return false;
       }
   }

   /** Default cache limit, see {@link #setCacheLimit(int)} */
   public static final int DEFAULT_CACHE_LIMIT = 256;

   public final AffineTransform tempT1 = new AffineTransform();
   public final AffineTransform tempT2 = new AffineTransform();
   private final HashMap<Key, GLRegion> stringCacheMap = new HashMap<Key, GLRegion>(DEFAULT_CACHE_LIMIT);
   private final ArrayList<Key> stringCacheArray = new ArrayList<Key>(DEFAULT_CACHE_LIMIT);
   private int stringCacheLimit = DEFAULT_CACHE_LIMIT;
}
