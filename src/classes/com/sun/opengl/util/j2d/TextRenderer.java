/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.util.j2d;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.font.*;
import java.awt.geom.*;
import java.util.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.impl.packrect.*;

// For debugging purposes
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.*;
import com.sun.opengl.impl.*;
import com.sun.opengl.util.*;

/** Renders bitmapped Java 2D text into an OpenGL window with high
    performance, full Unicode support, and a simple API. Performs
    appropriate caching of text rendering results in an OpenGL texture
    internally to avoid repeated font rasterization. The caching is
    completely automatic, does not require any user intervention, and
    has no visible controls in the public API. <P>

    Using the {@link TextRenderer TextRenderer} is simple. Add a
    "<code>TextRenderer renderer;</code>" field to your {@link
    GLEventListener GLEventListener}. In your {@link
    GLEventListener#init init} method, add:

<PRE>
    renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
</PRE>

    <P> In the {@link GLEventListener#display display} method of your
    {@link GLEventListener GLEventListener}, add:
<PRE>
    renderer.beginRendering(drawable.getWidth(), drawable.getHeight());
    // optionally set the color
    renderer.setColor(1.0f, 0.2f, 0.2f, 0.8f);
    renderer.draw("Text to draw", xPosition, yPosition);
    // ... more draw commands, color changes, etc.
    renderer.endRendering();
</PRE>

    Unless you are sharing textures and display lists between OpenGL
    contexts, you do not need to call the {@link #dispose dispose}
    method of the TextRenderer; the OpenGL resources it uses
    internally will be cleaned up automatically when the OpenGL
    context is destroyed. <P>

    The TextRenderer can be used with the {@link
    com.sun.opengl.util.TileRenderer TileRenderer} to produce
    high-resolution screen shots. In this scenario, the TextRenderer's
    {@link #begin3DRendering begin3DRendering}, {@link #draw3D draw3D}
    and {@link #end3DRendering end3DRendering} methods must be used to
    draw the text since the TileRenderer requires that the modelview
    and projection matrices not be modified during the rendering
    process. <P>

    Internally, the renderer uses a rectangle packing algorithm to
    pack multiple full Strings' rendering results (which are variable
    size) onto a larger OpenGL texture. The internal backing store is
    maintained using a {@link TextureRenderer TextureRenderer}. A
    least recently used (LRU) algorithm is used to discard previously
    rendered strings; the specific algorithm is undefined, but is
    currently implemented by flushing unused Strings' rendering
    results every few hundred rendering cycles, where a rendering
    cycle is defined as a pair of calls to {@link #beginRendering
    beginRendering} / {@link #endRendering endRendering}.
*/

public class TextRenderer {
  private static final boolean DEBUG = Debug.debug("TextRenderer");

  private Font font;
  private boolean antialiased;
  private boolean useFractionalMetrics;

  // Whether we're attempting to use automatic mipmap generation support
  private boolean mipmap;

  private RectanglePacker packer;
  private boolean haveMaxSize;
  private RenderDelegate renderDelegate;
  private TextureRenderer cachedBackingStore;
  private Graphics2D cachedGraphics;
  private FontRenderContext cachedFontRenderContext;
  private Map/*<String,Rect>*/ stringLocations = new HashMap/*<String,Rect>*/();

  // Support tokenization of space-separated words
  // NOTE: not using this at the present time as we aren't producing
  // identical rendering results; may ultimately yield more efficient
  // use of the backing store
  // private boolean splitAtSpaces = !Debug.isPropertyDefined("jogl.TextRenderer.nosplit");
  private boolean splitAtSpaces = false;
  private int spaceWidth = -1;
  private List/*<String>*/ tokenizationResults = new ArrayList/*<String>*/();

  // Every certain number of render cycles, flush the strings which
  // haven't been used recently
  private static final int CYCLES_PER_FLUSH = 100;
  private int numRenderCycles;
  // The amount of vertical dead space on the backing store before we
  // force a compaction
  private static final float MAX_VERTICAL_FRAGMENTATION = 0.7f;

  // Data associated with each rectangle of text
  static class TextData {
    private String str;    // Back-pointer to String this TextData describes
    // The following must be defined and used VERY precisely. This is
    // the offset from the upper-left corner of this rectangle (Java
    // 2D coordinate system) at which the string must be rasterized in
    // order to fit within the rectangle -- the leftmost point of the
    // baseline.
    private Point origin;
    private boolean used;  // Whether this text was used recently

    TextData(String str, Point origin) {
      this.str = str;
      this.origin = origin;
    }

    String string()  { return str;    }
    Point origin()   { return origin; }
    boolean used()   { return used;   }
    void markUsed()  { used = true;   }
    void clearUsed() { used = false;  }
  }

  // Need to keep track of whether we're in a beginRendering() /
  // endRendering() cycle so we can re-enter the exact same state if
  // we have to reallocate the backing store
  private boolean inBeginEndPair;
  private boolean isOrthoMode;
  private int beginRenderingWidth;
  private int beginRenderingHeight;
  private boolean beginRenderingDepthTestDisabled;
  // For resetting the color after disposal of the old backing store
  private boolean haveCachedColor;
  private float cachedR;
  private float cachedG;
  private float cachedB;
  private float cachedA;
  private Color cachedColor;
  private boolean needToResetColor;

  // For debugging only
  private Frame dbgFrame;

  /** Class supporting more full control over the process of rendering
      the bitmapped text. Allows customization of whether the backing
      store text bitmap is full-color or intensity only, the size of
      each individual rendered text rectangle, and the contents of
      each individual rendered text string. The default implementation
      of this interface uses an intensity-only texture, a
      closely-cropped rectangle around the text, and renders text
      using the color white, which is modulated by the set color
      during the rendering process. */
  public static interface RenderDelegate {
    /** Indicates whether the backing store of this TextRenderer
        should be intensity-only (the default) or full-color. */
    public boolean intensityOnly();

    /** Computes the bounds of the given text string relative to the
        origin. */
    public Rectangle2D getBounds(String str,
                                 Font font,
                                 FontRenderContext frc);

    /** Render the passed String at the designated location using the
        supplied Graphics2D instance. The surrounding region will
        already have been cleared to the RGB color (0, 0, 0) with zero
        alpha. The initial drawing context of the passed Graphics2D
        will be set to use AlphaComposite.SrcOver, the color white,
        the Font specified in the TextRenderer's constructor, and the
        rendering hints specified in the TextRenderer constructor.
        Changes made by the end user may be visible in successive
        calls to this method, but are not guaranteed to be preserved.
        Implementors of this method should reset the Graphics2D's
        state to that desired each time this method is called, in
        particular those states which are not the defaults. */
    public void draw(Graphics2D graphics, String str, int x, int y);
 }

  // Debugging purposes only
  private boolean debugged;

  /** Creates a new TextRenderer with the given font, using no
      antialiasing or fractional metrics, and the default
      RenderDelegate. Equivalent to <code>TextRenderer(font, false,
      false)</code>.

      @param font the font to render with
  */      
  public TextRenderer(Font font) {
    this(font, false, false, null, false);
  }

  /** Creates a new TextRenderer with the given font, using no
      antialiasing or fractional metrics, and the default
      RenderDelegate. If <CODE>mipmap</CODE> is true, attempts to use
      OpenGL's automatic mipmap generation for better smoothing when
      rendering the TextureRenderer's contents at a distance.
      Equivalent to <code>TextRenderer(font, false, false)</code>.

      @param font the font to render with
      @param mipmap whether to attempt use of automatic mipmap generation
  */      
  public TextRenderer(Font font, boolean mipmap) {
    this(font, false, false, null, mipmap);
  }

  /** Creates a new TextRenderer with the given Font, specified font
      properties, and default RenderDelegate. The
      <code>antialiased</code> and <code>useFractionalMetrics</code>
      flags provide control over the same properties at the Java 2D
      level. No mipmap support is requested. Equivalent to
      <code>TextRenderer(font, antialiased, useFractionalMetrics,
      null)</code>.

      @param font the font to render with
      @param antialiased whether to use antialiased fonts
      @param useFractionalMetrics whether to use fractional font
        metrics at the Java 2D level
  */
  public TextRenderer(Font font,
                      boolean antialiased,
                      boolean useFractionalMetrics) {
    this(font, antialiased, useFractionalMetrics, null, false);
  }

  /** Creates a new TextRenderer with the given Font, specified font
      properties, and given RenderDelegate. The
      <code>antialiased</code> and <code>useFractionalMetrics</code>
      flags provide control over the same properties at the Java 2D
      level. The <code>renderDelegate</code> provides more control
      over the text rendered. No mipmap support is requested.

      @param font the font to render with
      @param antialiased whether to use antialiased fonts
      @param useFractionalMetrics whether to use fractional font
        metrics at the Java 2D level
      @param renderDelegate the render delegate to use to draw the
        text's bitmap, or null to use the default one
  */
  public TextRenderer(Font font,
                      boolean antialiased,
                      boolean useFractionalMetrics,
                      RenderDelegate renderDelegate) {
    this(font, antialiased, useFractionalMetrics, renderDelegate, false);
  }

  /** Creates a new TextRenderer with the given Font, specified font
      properties, and given RenderDelegate. The
      <code>antialiased</code> and <code>useFractionalMetrics</code>
      flags provide control over the same properties at the Java 2D
      level. The <code>renderDelegate</code> provides more control
      over the text rendered. If <CODE>mipmap</CODE> is true, attempts
      to use OpenGL's automatic mipmap generation for better smoothing
      when rendering the TextureRenderer's contents at a distance.

      @param font the font to render with
      @param antialiased whether to use antialiased fonts
      @param useFractionalMetrics whether to use fractional font
        metrics at the Java 2D level
      @param renderDelegate the render delegate to use to draw the
        text's bitmap, or null to use the default one
      @param mipmap whether to attempt use of automatic mipmap generation
  */
  public TextRenderer(Font font,
                      boolean antialiased,
                      boolean useFractionalMetrics,
                      RenderDelegate renderDelegate,
                      boolean mipmap) {
    this.font = font;
    this.antialiased = antialiased;
    this.useFractionalMetrics = useFractionalMetrics;
    this.mipmap = mipmap;

    // FIXME: consider adjusting the size based on font size
    // (it will already automatically resize if necessary)
    packer = new RectanglePacker(new Manager(), 256, 256);

    if (renderDelegate == null) {
      renderDelegate = new DefaultRenderDelegate();
    }
    this.renderDelegate = renderDelegate;
  }

  /** Returns the bounding rectangle of the given String, assuming it
      was rendered at the origin. The coordinate system of the
      returned rectangle is Java 2D's, with increasing Y coordinates
      in the downward direction. The relative coordinate (0, 0) in the
      returned rectangle corresponds to the baseline of the leftmost
      character of the rendered string, in similar fashion to the
      results returned by, for example, {@link
      GlyphVector#getVisualBounds}. Most applications will use only
      the width and height of the returned Rectangle for the purposes
      of centering or justifying the String. It is not specified which
      Java 2D bounds ({@link GlyphVector#getVisualBounds
      getVisualBounds}, {@link GlyphVector#getPixelBounds
      getPixelBounds}, etc.) the returned bounds correspond to,
      although every effort is made to ensure an accurate bound. */
  public Rectangle2D getBounds(String str) {
    // FIXME: this doesn't hit the cache if tokenization is enabled --
    // needs more work
    // Prefer a more optimized approach
    Rect r = null;
    if ((r = (Rect) stringLocations.get(str)) != null) {
      TextData data = (TextData) r.getUserData();
      // Reconstitute the Java 2D results based on the cached values
      return new Rectangle2D.Double(-data.origin().x,
                                    -data.origin().y,
                                    r.w(), r.h());
    }

    // Must return a Rectangle compatible with the layout algorithm --
    // must be idempotent
    return normalize(renderDelegate.getBounds(str, font, getFontRenderContext()));
  }

  /** Returns the Font this renderer is using. */
  public Font getFont() {
    return font;
  }

  /** Returns a FontRenderContext which can be used for external
      text-related size computations. This object should be considered
      transient and may become invalidated between {@link
      #beginRendering beginRendering} / {@link #endRendering
      endRendering} pairs. */
  public FontRenderContext getFontRenderContext() {
    if (cachedFontRenderContext == null) {
      cachedFontRenderContext = getGraphics2D().getFontRenderContext();
    }
    return cachedFontRenderContext;
  }

  /** Begins rendering with this {@link TextRenderer TextRenderer}
      into the current OpenGL drawable, pushing the projection and
      modelview matrices and some state bits and setting up a
      two-dimensional orthographic projection with (0, 0) as the
      lower-left coordinate and (width, height) as the upper-right
      coordinate. Binds and enables the internal OpenGL texture
      object, sets the texture environment mode to GL_MODULATE, and
      changes the current color to the last color set with this
      TextRenderer via {@link #setColor setColor}. This method
      disables the depth test and is equivalent to
      beginRendering(width, height, true).

      @param width the width of the current on-screen OpenGL drawable
      @param height the height of the current on-screen OpenGL drawable
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void beginRendering(int width, int height) throws GLException {
    beginRendering(width, height, true);
  }

  /** Begins rendering with this {@link TextRenderer TextRenderer}
      into the current OpenGL drawable, pushing the projection and
      modelview matrices and some state bits and setting up a
      two-dimensional orthographic projection with (0, 0) as the
      lower-left coordinate and (width, height) as the upper-right
      coordinate. Binds and enables the internal OpenGL texture
      object, sets the texture environment mode to GL_MODULATE, and
      changes the current color to the last color set with this
      TextRenderer via {@link #setColor setColor}. Disables the depth
      test if the disableDepthTest argument is true.

      @param width the width of the current on-screen OpenGL drawable
      @param height the height of the current on-screen OpenGL drawable
      @param disableDepthTest whether to disable the depth test
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void beginRendering(int width, int height, boolean disableDepthTest) throws GLException {
    beginRendering(true, width, height, disableDepthTest);
  }

  /** Begins rendering of 2D text in 3D with this {@link TextRenderer
      TextRenderer} into the current OpenGL drawable. Assumes the end
      user is responsible for setting up the modelview and projection
      matrices, and will render text using the {@link #draw3D draw3D}
      method. This method pushes some OpenGL state bits, binds and
      enables the internal OpenGL texture object, sets the texture
      environment mode to GL_MODULATE, and changes the current color
      to the last color set with this TextRenderer via {@link
      #setColor setColor}.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void begin3DRendering() throws GLException {
    beginRendering(false, 0, 0, false);
  }

  /** Changes the current color of this TextRenderer to the supplied
      one. The default color is opaque white.

      @param color the new color to use for rendering text
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void setColor(Color color) throws GLException {
    getBackingStore().setColor(color);
    haveCachedColor = true;
    cachedColor = color;
  }

  /** Changes the current color of this TextRenderer to the supplied
      one, where each component ranges from 0.0f - 1.0f. The alpha
      component, if used, does not need to be premultiplied into the
      color channels as described in the documentation for {@link
      com.sun.opengl.util.texture.Texture Texture}, although
      premultiplied colors are used internally. The default color is
      opaque white.

      @param r the red component of the new color
      @param g the green component of the new color
      @param b the blue component of the new color
      @param a the alpha component of the new color, 0.0f = completely
        transparent, 1.0f = completely opaque
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void setColor(float r, float g, float b, float a) throws GLException {
    getBackingStore().setColor(r, g, b, a);
    haveCachedColor = true;
    cachedR = r;
    cachedG = g;
    cachedB = b;
    cachedA = a;
    cachedColor = null;
  }

  /** Draws the supplied String at the desired location using the
      renderer's current color. The baseline of the leftmost character
      is at position (x, y) specified in OpenGL coordinates, where the
      origin is at the lower-left of the drawable and the Y coordinate
      increases in the upward direction.

      @param str the string to draw
      @param x the x coordinate at which to draw
      @param y the y coordinate at which to draw
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void draw(String str, int x, int y) throws GLException {
    draw3D(str, x, y, 0, 1);
  }

  /** Draws the supplied String at the desired 3D location using the
      renderer's current color. The baseline of the leftmost character
      is placed at position (x, y, z) in the current coordinate system.

      @param str the string to draw
      @param x the x coordinate at which to draw
      @param y the y coordinate at which to draw
      @param z the z coordinate at which to draw
      @param scaleFactor a uniform scale factor applied to the width and height of the drawn rectangle
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void draw3D(String str,
                     float x, float y, float z,
                     float scaleFactor) {
    // Split up the string into space-separated pieces
    tokenize(str);
    int xOffset = 0;
    for (Iterator iter = tokenizationResults.iterator(); iter.hasNext(); ) {
      String curStr = (String) iter.next();
      if (curStr != null) {
        // Look up the string on the backing store
        Rect rect = (Rect) stringLocations.get(curStr);
        if (rect == null) {
          // Rasterize this string and place it on the backing store
          Graphics2D g = getGraphics2D();
          Rectangle2D bbox =
            normalize(renderDelegate.getBounds(curStr, font,
                                               getFontRenderContext()));
          Point origin = new Point((int) -bbox.getMinX(),
                                   (int) -bbox.getMinY());
          rect = new Rect(0, 0,
                          (int) bbox.getWidth(),
                          (int) bbox.getHeight(),
                          new TextData(curStr, origin));
          packer.add(rect);
          stringLocations.put(curStr, rect);
          // Re-fetch the Graphics2D in case the addition of the rectangle
          // caused the old backing store to be thrown away
          g = getGraphics2D();
          // OK, should now have an (x, y) for this rectangle; rasterize
          // the String
          // FIXME: need to verify that this causes the String to be
          // rasterized fully into the bounding rectangle
          int strx = rect.x() + origin.x;
          int stry = rect.y() + origin.y;
          // Clear out the area we're going to draw into
          Composite composite = g.getComposite();
          g.setComposite(AlphaComposite.Clear);
          g.fillRect(rect.x(), rect.y(), rect.w(), rect.h());
          g.setComposite(composite);
          // Draw the string
          renderDelegate.draw(g, curStr, strx, stry);
          // Mark this region of the TextureRenderer as dirty
          getBackingStore().markDirty(rect.x(), rect.y(), rect.w(), rect.h());
        }

        // OK, now draw the portion of the backing store to the screen
        TextureRenderer renderer = getBackingStore();
        // NOTE that the rectangles managed by the packer have their
        // origin at the upper-left but the TextureRenderer's origin is
        // at its lower left!!!
        TextData data = (TextData) rect.getUserData();
        data.markUsed();

        // Align the leftmost point of the baseline to the (x, y, z) coordinate requested
        renderer.draw3DRect(x + xOffset - scaleFactor * data.origin().x,
                            y - scaleFactor * ((rect.h() - data.origin().y)),
                            z,
                            rect.x(),
                            renderer.getHeight() - rect.y() - rect.h(),
                            rect.w(), rect.h(),
                            scaleFactor);
        xOffset += rect.w() * scaleFactor;
      }
      xOffset += getSpaceWidth() * scaleFactor;
    }
  }

  /** Ends a render cycle with this {@link TextRenderer TextRenderer}.
      Restores the projection and modelview matrices as well as
      several OpenGL state bits. Should be paired with {@link
      #beginRendering beginRendering}.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void endRendering() throws GLException {
    endRendering(true);
  }

  /** Returns the width of the ASCII space character, in pixels, drawn
      in this TextRenderer's font when no scaling or rotation has been
      applied. This is the horizontal advance of the space character.

      @return the width of the space character in the TextRenderer's font
  */
  private int getSpaceWidth() {
    if (spaceWidth < 0) {
      Graphics2D g = getGraphics2D();
      FontRenderContext frc = getFontRenderContext();
      GlyphVector gv = font.createGlyphVector(frc, " ");
      GlyphMetrics metrics = gv.getGlyphMetrics(0);
      spaceWidth = (int) metrics.getAdvanceX();
    }
    return spaceWidth;
  }

  /** Ends a 3D render cycle with this {@link TextRenderer TextRenderer}.
      Restores several OpenGL state bits. Should be paired with {@link
      #begin3DRendering begin3DRendering}.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void end3DRendering() throws GLException {
    endRendering(false);
  }

  /** Disposes of all resources this TextRenderer is using. It is not
      valid to use the TextRenderer after this method is called.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void dispose() throws GLException {
    packer.dispose();
    packer = null;
    cachedBackingStore = null;
    cachedGraphics = null;
    cachedFontRenderContext = null;
    if (dbgFrame != null) {
      dbgFrame.dispose();
    }
  }
  
  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static Rectangle2D normalize(Rectangle2D src) {
    // Give ourselves a one-pixel boundary around each string in order
    // to prevent bleeding of nearby Strings due to the fact that we
    // use linear filtering
    return new Rectangle2D.Double((int) Math.floor(src.getMinX() - 1),
                                  (int) Math.floor(src.getMinY() - 1),
                                  (int) Math.ceil(src.getWidth() + 2),
                                  (int) Math.ceil(src.getHeight()) + 2);
  }

  private TextureRenderer getBackingStore() {
    TextureRenderer renderer = (TextureRenderer) packer.getBackingStore();
    if (renderer != cachedBackingStore) {
      // Backing store changed since last time; discard any cached Graphics2D
      if (cachedGraphics != null) {
        cachedGraphics.dispose();
        cachedGraphics = null;
        cachedFontRenderContext = null;
      }
      cachedBackingStore = renderer;
    }
    return cachedBackingStore;
  }

  private Graphics2D getGraphics2D() {
    TextureRenderer renderer = getBackingStore();
    if (cachedGraphics == null) {
      cachedGraphics = renderer.createGraphics();
      // Set up composite, font and rendering hints
      cachedGraphics.setComposite(AlphaComposite.SrcOver);
      cachedGraphics.setColor(Color.WHITE);
      cachedGraphics.setFont(font);
      cachedGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                      (antialiased ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                                                   : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF));
      cachedGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                                      (useFractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                                                            : RenderingHints.VALUE_FRACTIONALMETRICS_OFF));
    }
    return cachedGraphics;
  }

  private void beginRendering(boolean ortho, int width, int height, boolean disableDepthTestForOrtho) {
    if (DEBUG && !debugged) {
      debug();
    }

    inBeginEndPair = true;
    isOrthoMode = ortho;
    beginRenderingWidth = width;
    beginRenderingHeight = height;
    beginRenderingDepthTestDisabled = disableDepthTestForOrtho;
    if (ortho) {
      getBackingStore().beginOrthoRendering(width, height, disableDepthTestForOrtho);
    } else {
      getBackingStore().begin3DRendering();
    }
    GL gl = GLU.getCurrentGL();

    if (!haveMaxSize) {
      // Query OpenGL for the maximum texture size and set it in the
      // RectanglePacker to keep it from expanding too large
      int[] sz = new int[1];
      gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, sz, 0);
      packer.setMaxSize(sz[0], sz[0]);
      haveMaxSize = true;
    }

    if (needToResetColor && haveCachedColor) {
      if (cachedColor == null) {
        getBackingStore().setColor(cachedR, cachedG, cachedB, cachedA);
      } else {
        getBackingStore().setColor(cachedColor);
      }
      needToResetColor = false;
    }

    // Disable future attempts to use mipmapping if TextureRenderer
    // doesn't support it
    if (mipmap && !getBackingStore().isUsingAutoMipmapGeneration()) {
      if (DEBUG) {
        System.err.println("Disabled mipmapping in TextRenderer");
      }
      mipmap = false;
    }
  }

  private void endRendering(boolean ortho) throws GLException {
    inBeginEndPair = false;
    if (ortho) {
      getBackingStore().endOrthoRendering();
    } else {
      getBackingStore().end3DRendering();
    }
    if (++numRenderCycles >= CYCLES_PER_FLUSH) {
      numRenderCycles = 0;
      if (DEBUG) {
        System.err.println("Clearing unused entries in endRendering()");
      }
      clearUnusedEntries();
    }
  }

  private void tokenize(String str) {
    // Avoid lots of little allocations per render
    tokenizationResults.clear();
    if (!splitAtSpaces) {
      tokenizationResults.add(str);
    } else {
      int startChar = 0;
      char c = (char) 0;
      int len = str.length();
      int i = 0;
      while (i < len) {
        if (str.charAt(i) == ' ') {
          // Terminate any substring
          if (startChar < i) {
            tokenizationResults.add(str.substring(startChar, i));
          } else {
            tokenizationResults.add(null);
          }
          startChar = i + 1;
        }
        ++i;
      }
      // Add on any remaining (all?) characters
      if (startChar == 0) {
        tokenizationResults.add(str);
      } else if (startChar < len) {
        tokenizationResults.add(str.substring(startChar, len));
      }
    }
  }

  private void clearUnusedEntries() {
    final List/*<Rect>*/ deadRects = new ArrayList/*<Rect>*/();
    // Iterate through the contents of the backing store, removing
    // text strings that haven't been used recently
    packer.visit(new RectVisitor() {
        public void visit(Rect rect) {
          TextData data = (TextData) rect.getUserData();
          if (data.used()) {
            data.clearUsed();
          } else {
            deadRects.add(rect);
          }
        }
      });
    for (Iterator iter = deadRects.iterator(); iter.hasNext(); ) {
      Rect r = (Rect) iter.next();
      packer.remove(r);
      stringLocations.remove(((TextData) r.getUserData()).string());

      if (DEBUG) {
        Graphics2D g = getGraphics2D();
        Composite composite = g.getComposite();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(r.x(), r.y(), r.w(), r.h());
        g.setComposite(composite);
      }
    }

    // If we removed dead rectangles this cycle, try to do a compaction
    float frag = packer.verticalFragmentationRatio();
    if (!deadRects.isEmpty() && frag > MAX_VERTICAL_FRAGMENTATION) {
      if (DEBUG) {
        System.err.println("Compacting TextRenderer backing store due to vertical fragmentation " + frag);
      }
      packer.compact();
    }

    if (DEBUG) {
      getBackingStore().markDirty(0, 0, getBackingStore().getWidth(), getBackingStore().getHeight());
    }
  }

  class Manager implements BackingStoreManager {
    private Graphics2D g;

    public Object allocateBackingStore(int w, int h) {
      // FIXME: should consider checking Font's attributes to see
      // whether we're likely to need to support a full RGBA backing
      // store (i.e., non-default Paint, foreground color, etc.), but
      // for now, let's just be more efficient
      TextureRenderer renderer;

      if (renderDelegate.intensityOnly()) {
        renderer = TextureRenderer.createAlphaOnlyRenderer(w, h, mipmap);
      } else {
        renderer = new TextureRenderer(w, h, true, mipmap);
      }
      if (DEBUG) {
        System.err.println(" TextRenderer allocating backing store " + w + " x " + h);
      }
      return renderer;
    }

    public void deleteBackingStore(Object backingStore) {
      ((TextureRenderer) backingStore).dispose();
    }

    public boolean preExpand(Rect cause, int attemptNumber) {
      // Only try this one time; clear out potentially obsolete entries

      // NOTE: this heuristic and the fact that it clears the used bit
      // of all entries seems to cause cycling of entries in some
      // situations, where the backing store becomes small compared to
      // the amount of text on the screen (see the TextFlow demo) and
      // the entries continually cycle in and out of the backing
      // store, decreasing performance. If we added a little age
      // information to the entries, and only cleared out entries
      // above a certain age, this behavior would be eliminated.
      // However, it seems the system usually stabilizes itself, so
      // for now we'll just keep things simple. Note that if we don't
      // clear the used bit here, the backing store tends to increase
      // very quickly to its maximum size, at least with the TextFlow
      // demo when the text is being continually re-laid out.
      if (attemptNumber == 0) {
        if (DEBUG) {
          System.err.println("Clearing unused entries in preExpand(): attempt number " + attemptNumber);
        }
        clearUnusedEntries();
        return true;
      }

      return false;
    }

    public void additionFailed(Rect cause, int attemptNumber) {
      // Heavy hammer -- might consider doing something different
      packer.clear();
      stringLocations.clear();

      if (DEBUG) {
        System.err.println(" *** Cleared all text because addition failed ***");
      }
    }

    public void beginMovement(Object oldBackingStore, Object newBackingStore) {
      // Exit the begin / end pair if necessary
      if (inBeginEndPair) {
        if (isOrthoMode) {
          ((TextureRenderer) oldBackingStore).endOrthoRendering();
        } else {
          ((TextureRenderer) oldBackingStore).end3DRendering();
        }
      }
      TextureRenderer newRenderer = (TextureRenderer) newBackingStore;
      g = newRenderer.createGraphics();
      // This is needed in particular for the case where the
      // RenderDelegate is using a non-intensity texture and therefore
      // has an alpha channel
      g.setComposite(AlphaComposite.Src);
    }

    public void move(Object oldBackingStore,
                     Rect   oldLocation,
                     Object newBackingStore,
                     Rect   newLocation) {
      TextureRenderer oldRenderer = (TextureRenderer) oldBackingStore;
      TextureRenderer newRenderer = (TextureRenderer) newBackingStore;

      if (oldRenderer == newRenderer) {
        // Movement on the same backing store -- easy case
        g.copyArea(oldLocation.x(), oldLocation.y(),
                   oldLocation.w(), oldLocation.h(),
                   newLocation.x() - oldLocation.x(),
                   newLocation.y() - oldLocation.y());
      } else {
        // Need to draw from the old renderer's image into the new one
        Image img = oldRenderer.getImage();
        g.drawImage(img,
                    newLocation.x(), newLocation.y(),
                    newLocation.x() + newLocation.w(), newLocation.y() + newLocation.h(),
                    oldLocation.x(), oldLocation.y(),
                    oldLocation.x() + oldLocation.w(), oldLocation.y() + oldLocation.h(),
                    null);
      }
    }

    public void endMovement(Object oldBackingStore, Object newBackingStore) {
      g.dispose();
      // Sync the whole surface
      TextureRenderer newRenderer = (TextureRenderer) newBackingStore;
      newRenderer.markDirty(0, 0, newRenderer.getWidth(), newRenderer.getHeight());
      // Re-enter the begin / end pair if necessary
      if (inBeginEndPair) {
        if (isOrthoMode) {
          ((TextureRenderer) newBackingStore).beginOrthoRendering(beginRenderingWidth,
                                                                  beginRenderingHeight,
                                                                  beginRenderingDepthTestDisabled);
        } else {
          ((TextureRenderer) newBackingStore).begin3DRendering();
        }
        if (haveCachedColor) {
          if (cachedColor == null) {
            ((TextureRenderer) newBackingStore).setColor(cachedR, cachedG, cachedB, cachedA);
          } else {
            ((TextureRenderer) newBackingStore).setColor(cachedColor);
          }
        }
      } else {
        needToResetColor = true;
      }
    }
  }

  class DefaultRenderDelegate implements RenderDelegate {
    public boolean intensityOnly() {
      return true;
    }

    public Rectangle2D getBounds(String str,
                                 Font font,
                                 FontRenderContext frc) {
      GlyphVector gv = font.createGlyphVector(frc, str);
      return gv.getPixelBounds(frc, 0, 0);
    }
    
    public void draw(Graphics2D graphics, String str, int x, int y) {
      graphics.drawString(str, x, y);
    }
  }

  //----------------------------------------------------------------------
  // Debugging functionality
  //

  private void debug() {
    dbgFrame = new Frame("TextRenderer Debug Output");
    GLCanvas dbgCanvas = new GLCanvas(new GLCapabilities(), null, GLContext.getCurrent(), null);
    dbgCanvas.addGLEventListener(new DebugListener(dbgFrame));
    dbgFrame.add(dbgCanvas);
    final FPSAnimator anim = new FPSAnimator(dbgCanvas, 10);
    dbgFrame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          // Run this on another thread than the AWT event queue to
          // make sure the call to Animator.stop() completes before
          // exiting
          new Thread(new Runnable() {
              public void run() {
                anim.stop();
              }
            }).start();
        }
      });
    dbgFrame.setSize(256, 256);
    dbgFrame.setVisible(true);
    anim.start();
    debugged = true;
  }

  class DebugListener implements GLEventListener {
    private GLU glu = new GLU();
    private Frame frame;

    DebugListener(Frame frame) {
      this.frame = frame;
    }

    public void display(GLAutoDrawable drawable) {
      GL gl = drawable.getGL();
      gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);
      if (packer == null)
        return;
      TextureRenderer rend = getBackingStore();
      final int w = rend.getWidth();
      final int h = rend.getHeight();
      rend.beginOrthoRendering(w, h);
      rend.drawOrthoRect(0, 0);
      rend.endOrthoRendering();
      if (frame.getWidth() != w ||
          frame.getHeight() != h) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
              frame.setSize(w, h);
            }
          });
      }
    }

    // Unused methods
    public void init(GLAutoDrawable drawable) {}
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
  }
}
