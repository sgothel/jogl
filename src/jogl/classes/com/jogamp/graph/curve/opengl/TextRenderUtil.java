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
import java.util.List;

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLException;

import jogamp.graph.geom.plane.AffineTransform;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.Font.Glyph;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

/**
 *
 * FIXME: Add VBO Vertex Factory for drawString3D !
 *
 */
public class TextRenderUtil {

    public final RegionRenderer renderer;

    public TextRenderUtil(final RegionRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Generate a Region to represent this Object.
     * <p>
     * Each glyph is cached and reused.
     * </p>
     *
     * @param renderModes bit-field of modes, e.g. {@link Region#VARIABLE_CURVE_WEIGHT_BIT}, {@link Region#VBAA_RENDERING_BIT}
     * @param vertexFactory vertex impl factory {@link Factory}
     * @param font the target {@link Font}
     * @param str string text
     * @param pixelSize
     */
    public static GLRegion createRegion(final int renderModes, final Factory<? extends Vertex> vertexFactory,
                                        final Font font, final CharSequence str, final int pixelSize) {
        final int charCount = str.length();

        final GLRegion region = Region.create(renderModes);
        // region.setFlipped(true);
        final Font.Metrics metrics = font.getMetrics();

        final float lineGap = metrics.getLineGap(pixelSize) ;
        final float ascent = metrics.getAscent(pixelSize) ;
        final float descent = metrics.getDescent(pixelSize) ;
        final float advanceY = lineGap - descent + ascent;
        final float scale = metrics.getScale(pixelSize);
        final AffineTransform transform = new AffineTransform(vertexFactory);
        final AffineTransform t = new AffineTransform(vertexFactory);

        float y = 0;
        float advanceTotal = 0;
        int numVertices = region.getNumVertices();

        for(int i=0; i< charCount; i++) {
            final char character = str.charAt(i);
            if( '\n' == character ) {
                y += advanceY;
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
                // glyphShape.closeLastOutline();

                if( false ) {
                    region.addOutlineShape(glyphShape, t);
                } else {
                    // System.err.println("XXXXX Pre TRI");
                    // glyphShape.getVertices();
                    final ArrayList<Triangle> trisIn = glyphShape.getTriangles(OutlineShape.VerticesState.QUADRATIC_NURBS);
                    final ArrayList<Vertex> gVertices = glyphShape.getVertices();

                    if( gVertices.size() < 3 ) {
                        continue;
                    }
                    region.addTriangles(trisIn, t, numVertices);

                    for(int j=0; j<gVertices.size(); j++) {
                        final Vertex vert = gVertices.get(j);
                        final Vertex svert = t.transform(vert, null);
                        svert.setId(numVertices++);
                        if(Region.DEBUG_INSTANCE) {
                            System.err.println("IN: "+vert);
                            System.err.println("EX: "+svert);
                        }
                        region.addVertex(svert, null);
                    }
                }
                assert( numVertices == region.getNumVertices() );

                advanceTotal += glyph.getAdvance(pixelSize, true);
            }
        }
        return region;
    }

    private static GLRegion createRegion2(final int renderModes, final Factory<? extends Vertex> vertexFactory,
                                         final Font font, final CharSequence str, final int pixelSize) {
        final List<OutlineShape> shapesIn = font.getOutlineShapes(null, str, pixelSize, vertexFactory);
        final ArrayList<OutlineShape> shapesOut = new ArrayList<OutlineShape>();
        final int numGlyps = shapesIn.size();
        for (int index=0;index<numGlyps;index++){
            if(shapesIn.get(index) == null){
                continue;
            }
            final OutlineShape glyphShape = shapesIn.get(index);

            if(glyphShape.getVertices().size() < 3) {
                continue;
            }
            shapesOut.add(glyphShape);
        }

        final GLRegion region = Region.create(renderModes);
        // region.setFlipped(true);
        int numVertices = region.getNumVertices();

        for(int i=0; i< shapesOut.size(); i++) {
            final OutlineShape shape = shapesOut.get(i);
            ArrayList<Triangle> gtris = shape.getTriangles(OutlineShape.VerticesState.QUADRATIC_NURBS);
            region.addTriangles(gtris, null, 0);

            final ArrayList<Vertex> gVertices = shape.getVertices();
            for(int j=0; j<gVertices.size(); j++) {
                final Vertex gVert = gVertices.get(j);
                gVert.setId(numVertices++);
                region.addVertex(gVert, null);
            }
        }
        return region;
    }


    /**
     * Render the String in 3D space wrt to the font provided at the position provided
     * the outlines will be generated, if not yet generated
     * @param gl the current GL state
     * @param font {@link Font} to be used
     * @param str text to be rendered
     * @param pixelSize font size
     * @param texWidth desired texture width for multipass-rendering.
     *        The actual used texture-width is written back when mp rendering is enabled, otherwise the store is untouched.
     * @throws Exception if TextRenderer not initialized
     */
    public void drawString3D(final GL2ES2 gl,
                             final Font font, final CharSequence str, final int pixelSize, final int[/*1*/] texSize) {
        if(!renderer.isInitialized()){
            throw new GLException("TextRendererImpl01: not initialized!");
        }
        final RenderState rs = renderer.getRenderState();
        GLRegion region = getCachedRegion(font, str, pixelSize);
        if(null == region) {
            region = createRegion(renderer.getRenderModes(), rs.getVertexFactory(), font, str, pixelSize);
            addCachedRegion(gl, font, str, pixelSize, region);
        }
        region.draw(gl, renderer, texSize);
    }

    /**
     * Render the String in 3D space wrt to the font provided at the position provided
     * the outlines will be generated, if not yet generated
     * @param gl the current GL state
     * @param font {@link Font} to be used
     * @param str text to be rendered
     * @param fontSize font size
     * @param texWidth desired texture width for multipass-rendering.
     *        The actual used texture-width is written back when mp rendering is enabled, otherwise the store is untouched.
     * @throws Exception if TextRenderer not initialized
     */
    public static void drawString3D(final RegionRenderer renderer, final GL2ES2 gl,
                                    final Font font, final CharSequence str, final int fontSize, final int[/*1*/] texSize) {
        if(!renderer.isInitialized()){
            throw new GLException("TextRendererImpl01: not initialized!");
        }
        final RenderState rs = renderer.getRenderState();
        GLRegion region = createRegion(renderer.getRenderModes(), rs.getVertexFactory(), font, str, fontSize);
        region.draw(gl, renderer, texSize);
    }

    /** FIXME
   public void flushCache(GL2ES2 gl) {
       Iterator<GlyphString> iterator = stringCacheMap.values().iterator();
       while(iterator.hasNext()){
           GlyphString glyphString = iterator.next();
           glyphString.destroy(gl, rs);
       }
       stringCacheMap.clear();
       stringCacheArray.clear();
   } */

   public void destroy(GL2ES2 gl) {
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

   protected final GLRegion getCachedRegion(Font font, CharSequence str, int fontSize) {
       return stringCacheMap.get(getKey(font, str, fontSize));
   }

   protected final void addCachedRegion(GL2ES2 gl, Font font, CharSequence str, int fontSize, GLRegion glyphString) {
       if ( 0 != getCacheLimit() ) {
           final String key = getKey(font, str, fontSize);
           final GLRegion oldRegion = stringCacheMap.put(key, glyphString);
           if ( null == oldRegion ) {
               // new entry ..
               validateCache(gl, 1);
               stringCacheArray.add(stringCacheArray.size(), key);
           } /// else overwrite is nop ..
       }
   }

   protected final void removeCachedRegion(GL2ES2 gl, Font font, CharSequence str, int fontSize) {
       final String key = getKey(font, str, fontSize);
       GLRegion region = stringCacheMap.remove(key);
       if(null != region) {
           region.destroy(gl, renderer);
       }
       stringCacheArray.remove(key);
   }

   protected final void removeCachedRegion(GL2ES2 gl, int idx) {
       final String key = stringCacheArray.remove(idx);
       final GLRegion region = stringCacheMap.remove(key);
       if(null != region) {
           region.destroy(gl, renderer);
       }
   }

   protected final String getKey(Font font, CharSequence str, int fontSize) {
       final StringBuilder sb = new StringBuilder();
       return font.getName(sb, Font.NAME_UNIQUNAME)
              .append(".").append(str.hashCode()).append(".").append(fontSize).toString();
   }

   /** Default cache limit, see {@link #setCacheLimit(int)} */
   public static final int DEFAULT_CACHE_LIMIT = 256;

   private final HashMap<String, GLRegion> stringCacheMap = new HashMap<String, GLRegion>(DEFAULT_CACHE_LIMIT);
   private final ArrayList<String> stringCacheArray = new ArrayList<String>(DEFAULT_CACHE_LIMIT);
   private int stringCacheLimit = DEFAULT_CACHE_LIMIT;
}