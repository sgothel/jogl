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

package com.jogamp.opengl.util.awt;

import java.awt.Graphics2D;

import com.jogamp.opengl.*;

/** Provides a Java 2D overlay on top of an arbitrary GLDrawable,
    making it easier to do things like draw text and images on top of
    an OpenGL scene while still maintaining reasonably good
    efficiency. */

public class Overlay {
  private final GLDrawable drawable;
  private TextureRenderer renderer;
  private boolean contentsLost;

  /** Creates a new Java 2D overlay on top of the specified
      GLDrawable. */
  public Overlay(final GLDrawable drawable) {
    this.drawable = drawable;
  }

  /** Creates a {@link java.awt.Graphics2D Graphics2D} instance for
      rendering into the overlay. The returned object should be
      disposed of using the normal {@link java.awt.Graphics#dispose()
      Graphics.dispose()} method once it is no longer being used.

      @return a new {@link java.awt.Graphics2D Graphics2D} object for
        rendering into the backing store of this renderer
  */
  public Graphics2D createGraphics() {
    // Validate the size of the renderer against the current size of
    // the drawable
    validateRenderer();
    return renderer.createGraphics();
  }

  /** Indicates whether the Java 2D contents of the overlay were lost
      since the last time {@link #createGraphics} was called. This
      method should be called immediately after calling {@link
      #createGraphics} to see whether the entire contents of the
      overlay need to be redrawn or just the region the application is
      interested in updating.

      @return whether the contents of the overlay were lost since the
        last render
  */
  public boolean contentsLost() {
    return contentsLost;
  }

  /** Marks the given region of the overlay as dirty. This region, and
      any previously set dirty regions, will be automatically
      synchronized with the underlying Texture during the next {@link
      #draw draw} or {@link #drawAll drawAll} operation, at which
      point the dirty region will be cleared. It is not necessary for
      an OpenGL context to be current when this method is called.

      @param x the x coordinate (in Java 2D coordinates -- relative to
        upper left) of the region to update
      @param y the y coordinate (in Java 2D coordinates -- relative to
        upper left) of the region to update
      @param width the width of the region to update
      @param height the height of the region to update

      @throws GLException If an OpenGL context is not current when this method is called */
  public void markDirty(final int x, final int y, final int width, final int height) {
    renderer.markDirty(x, y, width, height);
  }

  /** Draws the entire contents of the overlay on top of the OpenGL
      drawable. This is a convenience method which encapsulates all
      portions of the rendering process; if this method is used,
      {@link #beginRendering}, {@link #endRendering}, etc. should not
      be used. This method should be called while the OpenGL context
      for the drawable is current, and after your OpenGL scene has
      been rendered.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void drawAll() throws GLException {
    beginRendering();
    draw(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
    endRendering();
  }

  /** Begins the OpenGL rendering process for the overlay. This is
      separated out so advanced applications can render independent
      pieces of the overlay to different portions of the drawable.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void beginRendering() throws GLException {
    renderer.beginOrthoRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
  }

  /** Ends the OpenGL rendering process for the overlay. This is
      separated out so advanced applications can render independent
      pieces of the overlay to different portions of the drawable.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void endRendering() throws GLException {
    renderer.endOrthoRendering();
  }

  /** Draws the specified sub-rectangle of the overlay on top of the
      OpenGL drawable. {@link #beginRendering} and {@link
      #endRendering} must be used in conjunction with this method to
      achieve proper rendering results. This method should be called
      while the OpenGL context for the drawable is current, and after
      your OpenGL scene has been rendered.

      @param x the lower-left x coordinate (relative to the lower left
        of the overlay) of the rectangle to draw
      @param y the lower-left y coordinate (relative to the lower left
        of the overlay) of the rectangle to draw
      @param width the width of the rectangle to draw
      @param height the height of the rectangle to draw

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void draw(final int x, final int y, final int width, final int height) throws GLException {
    draw(x, y, x, y, width, height);
  }

  /** Draws the specified sub-rectangle of the overlay at the
      specified x and y coordinate on top of the OpenGL drawable.
      {@link #beginRendering} and {@link #endRendering} must be used
      in conjunction with this method to achieve proper rendering
      results. This method should be called while the OpenGL context
      for the drawable is current, and after your OpenGL scene has
      been rendered.

      @param screenx the on-screen x coordinate at which to draw the rectangle
      @param screeny the on-screen y coordinate (relative to lower left) at
        which to draw the rectangle
      @param overlayx the x coordinate of the pixel in the overlay of
        the lower left portion of the rectangle to draw
      @param overlayy the y coordinate of the pixel in the overlay
        (relative to lower left) of the lower left portion of the
        rectangle to draw
      @param width the width of the rectangle to draw
      @param height the height of the rectangle to draw

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void draw(final int screenx, final int screeny,
                   final int overlayx, final int overlayy,
                   final int width, final int height) throws GLException {
    renderer.drawOrthoRect(screenx, screeny,
                           overlayx, overlayy,
                           width, height);
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void validateRenderer() {
    if (renderer == null) {
      renderer = new TextureRenderer(drawable.getSurfaceWidth(),
                                     drawable.getSurfaceHeight(),
                                     true);
      contentsLost = true;
    } else if (renderer.getWidth() != drawable.getSurfaceWidth() ||
               renderer.getHeight() != drawable.getSurfaceHeight()) {
      renderer.setSize(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
      contentsLost = true;
    } else {
      contentsLost = false;
    }
  }
}
