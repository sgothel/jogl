/*
 * Copyright 2012 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util.awt;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.util.awt.TextRenderer.RenderDelegate;
import com.jogamp.opengl.util.packrect.BackingStoreManager;
import com.jogamp.opengl.util.packrect.Rect;
import com.jogamp.opengl.util.packrect.RectVisitor;
import com.jogamp.opengl.util.packrect.RectanglePacker;
import com.jogamp.opengl.util.texture.TextureCoords;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.List;


/**
 * Storage of glyphs in an OpenGL texture.
 *
 * <p>Handles storing glyphs in a 2D texture and retrieving their coordinates.
 *
 * <p>The first step in using a <i>GlyphCache</i> is to make sure it's set up by
 * calling {@link #beginRendering(GL)}.  Then glyphs can be added using {@link
 * #upload(Glyph)}.  Each glyph will be packed efficiently into the texture with
 * a small amount of space around it using {@link RectanglePacker}.  When all
 * glyphs have been added, be sure to call {@link #update(GL)} or {@link
 * #endRendering(GL)} before trying to render with the texture, as the glyphs
 * are not actually drawn into the texture right away in order to increase
 * performance.  Texture coordinates of individual glyphs can be determined with
 * {@link #find(Glyph)}.  When reusing the glyph cache, {@link #contains(Glyph)}
 * should be called to make sure a glyph is not already stored.
 *
 * <p><em>Events fired when:</em>
 * <ul>
 *   <li>a glyph has not been used recently (CLEAN, glyph)
 *   <li>the backing store is going to be flushed
 * </ul>
 *
 * <p>GlyphCache is compatible with GL2 or GL3.
 *
 * @see TextureBackingStore
 */
final class GlyphCache implements TextureBackingStore.EventListener {

    // Whether or not glyph cache should print debugging information
    private static final boolean DEBUG = false;

    // Number used to determine size of cache based on font size
    private static final int FONT_SIZE_MULTIPLIER = 5;

    // How much fragmentation to allow before compacting
    private static final float MAX_VERTICAL_FRAGMENTATION = 0.7f;

    // Number of render cycles before clearing unused entries
    private static final int CYCLES_PER_FLUSH = 100;

    // Minimum size of backing store in pixels
    private static final int MIN_BACKING_STORE_SIZE = 256;

    // Delegate to render text
    private final RenderDelegate renderDelegate;

    // Observers of glyph cache
    private final List<EventListener> listeners;

    // Delegate to create textures
    private final TextureBackingStoreManager manager;

    // Delegate to position glyphs
    private final RectanglePacker packer;

    // Texture to draw into
    private TextureBackingStore backingStore;

    // Times cache has been used
    private int numRenderCycles;

    // True if done initializing
    private boolean ready;

    /**
     * Creates a glyph cache.
     *
     * @param font Font that was used to create glyphs that will be stored
     * @param rd Controller of rendering bitmapped text
     * @param antialias Whether to render glyphs with smooth edges
     * @param subpixel Whether to consider subpixel positioning
     * @param mipmap Whether to create multiple sizes for texture
     * @return New glyph cache instance
     * @throws UnsupportedOperationException if render delegate wants full color
     * @throws IllegalArgumentException if font or render delegate is <tt>null</tt>
     */
    static GlyphCache newInstance(final Font font,
                                  final RenderDelegate rd,
                                  final boolean antialias,
                                  final boolean subpixel,
                                  final boolean mipmap) {

        if (font == null) {
            throw new IllegalArgumentException("Font is null!");
        } else if (rd == null) {
            throw new IllegalArgumentException("Render delegate is null!");
        } else if (!rd.intensityOnly()) {
            throw new UnsupportedOperationException("Does not support full color!");
        }

        final GlyphCache gc = new GlyphCache(font, rd, antialias, subpixel, mipmap);
        gc.manager.addListener(gc);
        return gc;
    }

    /**
     * Constructs a glyph cache.
     *
     * @param font Font that was used to create glyphs that will be stored
     * @param rd Controller of rendering bitmapped text
     * @param antialias Whether to render glyphs with smooth edges
     * @param subpixel Whether to consider subpixel positioning
     * @param mipmap Whether to create multiple sizes of texture
     * @throws AssertionError if font or render delegate is <tt>null</tt>
     */
    private GlyphCache(final Font font,
                       final RenderDelegate rd,
                       final boolean antialias,
                       final boolean subpixel,
                       final boolean mipmap) {

        assert (font != null);
        assert (rd != null);
        assert (rd.intensityOnly());

        this.renderDelegate = rd;
        this.listeners = new ArrayList<EventListener>();
        this.manager = new TextureBackingStoreManager(font, antialias, subpixel, mipmap);
        this.packer = createPacker(font, manager);
        this.ready = false;
    }

    /**
     * Adds an observer that will be notified of cache events.
     *
     * @param listener Observer that wants to be notified of cache events
     * @throws AssertionError if listener is <tt>null</tt>
     */
    void addListener(final EventListener listener) {
        assert (listener != null);
        listeners.add(listener);
    }

    /**
     * Sets up the cache for rendering.
     *
     * <p>After calling this method the texture storing the glyphs will
     * be bound.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws UnsupportedOperationException if render delegate wants more than intensity
     * @throws GLException if glPixelStorei not fully supported
     */
    void beginRendering(final GL gl) {

        // Set up if first time rendering
        if (!ready) {
            validate(gl);
            setMaxSize(gl);
            ready = true;
        }

        // Bind the backing store
        final TextureBackingStore bs = getBackingStore();
        bs.bind(gl, GL.GL_TEXTURE0);
    }

    /**
     * Determines if a glyph is stored in the cache.
     *
     * @param glyph Glyph that may or may not be in cache
     * @return <tt>True</tt> if <tt>glyph</tt> is in the cache
     * @throws NullPointerException if glyph is <tt>null</tt>
     */
    boolean contains(final Glyph glyph) {
        return glyph.location != null;
    }

    /**
     * Destroys resources used by the cache.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     */
    void dispose(final GL gl) {
        packer.dispose();
        if (backingStore != null) {
            backingStore.dispose(gl);
            backingStore = null;
        }
    }

    /**
     * Finishes setting up the cache for rendering.
     *
     * <p>After calling this method, all uploaded glyphs will be
     * guaranteed to be present in the underlying OpenGL texture.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     */
    void endRendering(final GL gl) {

        // Update
        update(gl);

        // Check if reached render cycle limit
        if (++numRenderCycles >= CYCLES_PER_FLUSH) {
            numRenderCycles = 0;
            if (DEBUG) {
                System.err.println("Reached cycle limit.");
            }
            clearUnusedEntries();
        }
    }

    /**
     * Determines the texture coordinates of a glyph in the cache.
     *
     * <p><b>Notes:</b>
     * <ul>
     *   <li>Texture coordinates are in the range 0 to 1
     *   <li>Automatically marks the glyph as being used recently
     *   <li>If cache has been resized, coordinates are recalculated
     * </ul>
     *
     * @param glyph Glyph already in cache
     * @return Texture coordinates of glyph in the cache
     * @throws NullPointerException if glyph is <tt>null</tt>
     */
    TextureCoords find(final Glyph glyph) {

        // Mark the glyph as being used
        markGlyphLocationUsed(glyph);

        // Find the coordinates, recalculating if necessary
        if (glyph.coordinates == null) {
            computeCoordinates(glyph);
        }
        return glyph.coordinates;
    }

    /**
     * Forces the cache to update the underlying OpenGL texture.
     *
     * <p>After calling this method, all uploaded glyphs will be guaranteed to
     * be present in the underlying OpenGL texture.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     */
    void update(final GL gl) {
        final TextureBackingStore bs = getBackingStore();
        bs.update(gl);
    }

    /**
     * Stores a glyph in the cache.
     *
     * <p>Determines a place to put the glyph in the underlying OpenGL texture,
     * computes the glyph's texture coordinates for that position, and requests
     * the glyph be drawn into the texture.  (Note however that to increase
     * performance the glyph is not guaranteed to actually be in the texture
     * until {@link #update(GL)} or {@link #endRendering(GL)} is called.)
     *
     * @param glyph Glyph not already stored in cache
     * @throws NullPointerException if glyph is <tt>null</tt>
     */
    void upload(final Glyph glyph) {

        // Perform upload steps
        findLocation(glyph);
        computeCoordinates(glyph);
        drawInBackingStore(glyph);

        // Make sure it's marked as used
        markGlyphLocationUsed(glyph);
    }

    //------------------------------------------------------------------
    // Helpers
    //

    /**
     * Computes the normalized coordinates of the glyph's location.
     *
     * @param glyph Glyph being uploaded
     * @throws NullPointerException if glyph is <tt>null</tt>
     */
    private void computeCoordinates(final Glyph glyph) {

        // Determine dimensions in pixels
        final int cacheWidth = getWidth();
        final int cacheHeight = getHeight();
        final float left = getLeftBorderLocation(glyph);
        final float bottom = getBottomBorderLocation(glyph);

        // Convert to normalized texture coordinates
        final float l = left / cacheWidth;
        final float b = bottom / cacheHeight;
        final float r = (left + glyph.width) / cacheWidth;
        final float t = (bottom - glyph.height) / cacheHeight;

        // Store in glyph
        glyph.coordinates = new TextureCoords(l, b, r, t);
    }

    /**
     * Makes a packer for positioning glyphs.
     *
     * @param font Font used to make glyphs being stored
     * @param bsm Handler of packer events
     * @return Resulting packer
     * @throws NullPointerException if font is <tt>null</tt>
     */
    private static RectanglePacker createPacker(final Font font, final BackingStoreManager bsm) {
        final int size = findBackingStoreSizeForFont(font);
        RectanglePacker packer = new RectanglePacker(bsm, size, size, 1f);
        return packer;
    }

    /**
     * Draws a glyph into the backing store.
     *
     * @param glyph Glyph being uploaded
     * @throws NullPointerException if glyph is <tt>null</tt>
     */
    private void drawInBackingStore(final Glyph glyph) {

        // Get the backing store
        final TextureBackingStore bs = getBackingStore();

        // Clear the area
        final Rect loc = glyph.location;
        final int x = loc.x();
        final int y = loc.y();
        final int w = loc.w();
        final int h = loc.h();
        bs.clear(x, y, w, h);

        // Draw the text
        renderDelegate.drawGlyphVector(
                bs.getGraphics(),
                glyph.glyphVector,
                getLeftBaselineLocation(glyph),
                getBottomBaselineLocation(glyph));

        // Mark it dirty
        bs.mark(x, y, w, h);
    }

    /**
     * Returns initial size of cache for a font.
     *
     * @param font Font to create glyphs from
     * @throws NullPointerException if font is <tt>null</tt>
     */
    private static int findBackingStoreSizeForFont(final Font font) {
        return Math.max(MIN_BACKING_STORE_SIZE, font.getSize() * FONT_SIZE_MULTIPLIER);
    }

    /**
     * Finds a location in the backing store for the glyph.
     *
     * @param glyph Glyph being uploaded
     * @throws NullPointerException if glyph is <tt>null</tt>
     */
    private void findLocation(final Glyph glyph) {

        // Compute a rectangle that includes glyph's margin
        final int x = 0;
        final int y = 0;
        final int w = glyph.margin.left + ((int) glyph.width) + glyph.margin.right;
        final int h = glyph.margin.top + ((int) glyph.height) + glyph.margin.bottom;
        final Rect rect = new Rect(x, y, w, h, new TextData(glyph));

        // Pack it into the cache and store its location
        packer.add(rect);
        glyph.location = rect;
        markGlyphLocationUsed(glyph);
    }

    /**
     * Determines the maximum texture size supported.
     *
     * @param gl Current OpenGL context
     * @return Maximum texture size
     * @throws NullPointerException if context is <tt>null</tt>
     */
    private static int findMaxSize(final GL gl) {
        final int[] size = new int[1];
        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, size, 0);
        return size[0];
    }

    /**
     * Determines the location of a glyph's bottom baseline.
     *
     * @param g Glyph
     * @return Location of <tt>g</tt>'s bottom baseline
     * @throws NullPointerException if glyph is <tt>null</tt>
     */
    private int getBottomBaselineLocation(final Glyph g) {
        return (int) (g.location.y() + g.margin.top + g.ascent);
    }

    /**
     * Determines the location of a glyph's bottom border.
     *
     * @param g Glyph
     * @return Location of <tt>g</tt>'s bottom border
     * @throws NullPointerException if glyph is <tt>null</tt>
     */
    private int getBottomBorderLocation(final Glyph g) {
        return (int) (g.location.y() + g.margin.top + g.height);
    }

    /**
     * Determines the location of a glyph's left baseline.
     *
     * @param g Glyph
     * @return Location of <tt>g</tt>'s left baseline
     * @throws NullPointerException if glyph is <tt>null</tt>
     */
    private int getLeftBaselineLocation(final Glyph g) {
        return (int) (g.location.x() + g.margin.left - g.kerning);
    }

    /**
     * Determines the location of a glyph's left border.
     *
     * @param g Glyph
     * @return Location of <tt>g</tt>'s left border
     * @throws NullPointerException if glyph is <tt>null</tt>
     */
    private int getLeftBorderLocation(final Glyph g) {
        return g.location.x() + g.margin.left;
    }

    /**
     * Returns <tt>true</tt> if Non-Power-Of-Two textures are available.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     */
    static boolean isNpotTextureAvailable(final GL gl) {
        final String name = "GL_ARB_texture_non_power_of_two";
        return gl.isExtensionAvailable(name);
    }

    /**
     * Marks a glyph's location as used.
     *
     * @param glyph Glyph with location
     * @throws NullPointerException if glyph is <tt>null</tt>
     */
    static void markGlyphLocationUsed(final Glyph glyph) {
        ((TextData) glyph.location.getUserData()).markUsed();
    }

    /**
     * Changes the maximum size of the packer.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     */
    private void setMaxSize(final GL gl) {
        final int maxSize = findMaxSize(gl);
        packer.setMaxSize(maxSize, maxSize);
    }

    /**
     * Makes sure input is OK and required hardware features are available.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws UnsupportedOperationException if render delegate wants more than intensity
     * @throws GLException if glPixelStorei not fully supported
     */
    private void validate(final GL gl) {

        // Check the render delegate
        if (!renderDelegate.intensityOnly()) {
            throw new UnsupportedOperationException("Currently only supports intensity.");
        }
    }

    //------------------------------------------------------------------
    // Event processing
    //

    /**
     * Clears all the texture coordinates stored in glyphs.
     */
    private void clearTextureCoordinates() {

        if (DEBUG) {
            System.err.println("Clearing texture coordinates");
        }

        packer.visit(new RectVisitor() {
            public void visit(final Rect rect) {
                final Glyph glyph = ((TextData) rect.getUserData()).glyph;
                glyph.coordinates = null;
            }
        });
    }

    /**
     * Clears entries that haven't been used in awhile.
     */
    private void clearUnusedEntries() {

        // Notify
        if (DEBUG) {
            System.err.println("Trying to clear unused entries...");
        }

        // Find rectangles in backing store that haven't been used recently
        final List<Rect> deadRects = new ArrayList<Rect>();
        packer.visit(new RectVisitor() {
            public void visit(final Rect rect) {
                final TextData data = (TextData) rect.getUserData();
                if (data.used()) {
                    data.clearUsed();
                } else {
                    deadRects.add(rect);
                }
            }
        });

        // Remove each of those rectangles and notify we're doing it
        final TextureBackingStore bs = getBackingStore();
        for (final Rect rect : deadRects) {
            packer.remove(rect);
            final Glyph glyph = ((TextData) rect.getUserData()).glyph;
            glyph.location = null;
            fireEvent(EventType.CLEAN, glyph);
            if (DEBUG) {
                System.err.print("Cleared rectangle for glyph: ");
                if (glyph.str != null) {
                    System.err.println(glyph.str);
                } else {
                    System.err.println(glyph.character);
                }
                bs.clear(rect.x(), rect.y(), rect.w(), rect.h());
            }
        }

        // If we removed dead rectangles this cycle, try to do a compaction
        final float frag = packer.verticalFragmentationRatio();
        if (!deadRects.isEmpty() && (frag > MAX_VERTICAL_FRAGMENTATION)) {
            if (DEBUG) {
                System.err.println("Compacting due to fragmentation " + frag);
            }
            packer.compact();
        }

        // Force the backing store to update
        if (DEBUG) {
            bs.mark(0, 0, bs.getWidth(), bs.getHeight());
        }
    }

    /**
     * Sends an event to all the listeners.
     *
     * @param type Kind of event
     * @param data Information to send with event
     * @throws AssertionError if type or data is <tt>null</tt>
     */
    private void fireEvent(final EventType type, final Object data) {
        assert (type != null);
        assert (data != null);
        for (final EventListener listener : listeners) {
            listener.onGlyphCacheEvent(type, data);
        }
    }

    /**
     * Handles events coming from the backing store.
     *
     * @param type Kind of backing store event
     */
    @Override
    public void onBackingStoreEvent(final TextureBackingStore.EventType type) {
        switch (type) {
        case REALLOCATE:
            onBackingStoreReallocate();
            break;
        case FAILURE:
            onBackingStoreFailure();
            break;
        }
    }

    /**
     * Handles when a backing store could not be reallocated.
     *
     * <p>Clears the cache and notifies observers.
     */
    private void onBackingStoreFailure() {
        packer.clear();
        fireEvent(EventType.CLEAR, null);
    }

    /**
     * Handles when a backing store is reallocated.
     *
     * <p>First notifies observers, then tries to remove any unused entries, and
     * finally erases the texture coordinates of each entry since the width and
     * height of the total texture has changed.  Note that since the backing
     * store is just expanded without moving any entries, only the texture
     * coordinates need to be recalculated.  The locations will still be the
     * same.
     *
     * <p>This heuristic and the fact that it clears the used bit of all entries
     * seems to cause cycling of entries in some situations, where the backing
     * store becomes small compared to the amount of text on the screen (see the
     * TextFlow demo) and the entries continually cycle in and out of the
     * backing store, decreasing performance.  If we added a little age
     * information to the entries, and only cleared out entries above a certain
     * age, this behavior would be eliminated.  However, it seems the system
     * usually stabilizes itself, so for now we'll just keep things simple.
     * Note that if we don't clear the used bit here, the backing store tends to
     * increase very quickly to its maximum size, at least with the TextFlow
     * demo when the text is being continually re-laid out.
     */
    private void onBackingStoreReallocate() {
        fireEvent(EventType.REALLOCATE, null);
        clearUnusedEntries();
        clearTextureCoordinates();
    }

    //------------------------------------------------------------------
    // Getters and setters
    //

    /**
     * Returns object actually storing the rasterized glyphs.
     */
    TextureBackingStore getBackingStore() {
        return (TextureBackingStore) packer.getBackingStore();
    }

    /**
     * Returns font render context used for text size computations.
     *
     * <p>This object should be considered transient and may become
     * invalidated between {@link #beginRendering} and {@link
     * #endRendering} pairs.
     */
    FontRenderContext getFontRenderContext() {
        return getBackingStore().getGraphics().getFontRenderContext();
    }

    /**
     * Returns height of the entire cache.
     */
    int getHeight() {
        return getBackingStore().getHeight();
    }

    /**
     * Returns <tt>true</tt> if texture is interpolating when sampling.
     */
    boolean getUseSmoothing() {
        return ((TextureBackingStoreManager) manager).getUseSmoothing();
    }

    /**
     * Changes whether texture should interpolate when sampling.
     *
     * @param useSmoothing <tt>true</tt> to use linear interpolation
     */
    void setUseSmoothing(boolean useSmoothing) {
        ((TextureBackingStoreManager) manager).setUseSmoothing(useSmoothing);
        getBackingStore().setUseSmoothing(useSmoothing);
    }

    /**
     * Returns width of the entire cache.
     */
    int getWidth() {
        return getBackingStore().getWidth();
    }

    //-----------------------------------------------------------------
    // Nested classes
    //

    /**
     * Object that wants to be notified of cache events.
     */
    static interface EventListener {

        /**
         * Responds to an event from a glyph cache.
         *
         * @param type Type of event
         * @param data Object that triggered the event, i.e. a glyph
         * @throws NullPointerException if event type or data is <tt>null</tt>
         */
        public void onGlyphCacheEvent(EventType type, Object data);
    }

    /**
     * Type of event fired from the cache.
     */
    public static enum EventType {

        /**
         * All entries were removed from cache.
         */
        CLEAR,

        /**
         * Unused entries were removed from cache.
         */
        CLEAN,

        /**
         * Backing store changed size.
         */
        REALLOCATE
    };

    /**
     * Data associated with each rectangle of text.
     */
    static final class TextData {

        // Visual representation of text
        Glyph glyph;

        // True if text was used recently
        private boolean used;

        /**
         * Constructs a text data from a glyph.
         *
         * @param glyph Visual representation of text
         * @throws AssertionError if glyph is <tt>null</tt>
         */
        TextData(final Glyph glyph) {
            assert (glyph != null);
            this.glyph = glyph;
        }

        /**
         * Indicates the text data is no longer being used.
         */
        void clearUsed() {
            used = false;
        }

        /**
         * Indicates the text data was just used.
         */
        void markUsed() {
            used = true;
        }

        /**
         * Returns the actual text stored with a rectangle.
         */
        String string() {
            return glyph.str;
        }

        /**
         * Returns <tt>true</tt> if text has been used recently.
         */
        boolean used() {
            return used;
        }
    }
}
