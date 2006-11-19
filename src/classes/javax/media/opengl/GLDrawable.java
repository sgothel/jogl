/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

package javax.media.opengl;

// FIXME: We need some way to tell when the device upon which the canvas is
// being displayed has changed (e.g., the user drags the canvas's parent
// window from one screen on multi-screen environment to another, when the
// user changes the display bit depth or screen resolution, etc). When this
// occurs, we need the canvas to reset the gl function pointer tables for the
// canvas, because the new device may have different capabilities (e.g.,
// doesn't support as many opengl extensions) from the original device. This
// hook would also be useful in other GLDrawables (for example, offscreen
// buffers such as pbuffers, whose contents may or may not be invalidated when
// the display mode changes, depending on the vendor's GL implementation).
//
// Right now I'm not sure how hook into when this change occurs. There isn't
// any AWT event corresponding to a device change (as far as I can
// tell). We could constantly check the GraphicsConfiguration of the canvas's top-level
// parent to see if it has changed, but this would be very slow (we'd have to
// do it every time the context is made current). There has got to be a better
// solution, but I'm not sure what it is.

// FIXME: Subclasses need to call resetGLFunctionAvailability() on their
// context whenever the displayChanged() function is called on our
// GLEventListeners

/** An abstraction for an OpenGL rendering target. A GLDrawable's
    primary functionality is to create OpenGL contexts which can be
    used to perform rendering. A GLDrawable does not automatically
    create an OpenGL context, but all implementations of {@link
    GLAutoDrawable} do so upon creation. */

public interface GLDrawable {
  /**
   * Creates a new context for drawing to this drawable that will
   * optionally share display lists and other server-side OpenGL
   * objects with the specified GLContext. <P>
   *
   * The GLContext <code>share</code> need not be associated with this
   * GLDrawable and may be null if sharing of display lists and other
   * objects is not desired. See the note in the overview
   * documentation on
   * <a href="../../../overview-summary.html#SHARING">context sharing</a>.
   */
  public GLContext createContext(GLContext shareWith);

  /**

   * Indicates to on-screen GLDrawable implementations whether the
   * underlying window has been created and can be drawn into. This
   * method must be called from GLDrawables obtained from the
   * GLDrawableFactory via the {@link GLDrawableFactory#getGLDrawable
   * GLDrawableFactory.getGLDrawable()} method. It must typically be
   * called with an argument of <code>true</code> in the
   * <code>addNotify</code> method of components performing OpenGL
   * rendering and with an argument of <code>false</code> in the
   * <code>removeNotify</code> method. Calling this method has no
   * other effects. For example, if <code>removeNotify</code> is
   * called on a Canvas implementation for which a GLDrawable has been
   * created, it is also necessary to destroy all OpenGL contexts
   * associated with that GLDrawable. This is not done automatically
   * by the implementation. It is not necessary to call
   * <code>setRealized</code> on a GLCanvas, a GLJPanel, or a
   * GLPbuffer, as these perform the appropriate calls on their
   * underlying GLDrawables internally..
   */
  public void setRealized(boolean realized);

  /** Requests a new width and height for this GLDrawable. Not all
      drawables are able to respond to this request and may silently
      ignore it. */
  public void setSize(int width, int height);

  /** Returns the current width of this GLDrawable. */
  public int getWidth();

  /** Returns the current height of this GLDrawable. */
  public int getHeight();

  /** Swaps the front and back buffers of this drawable. For {@link
      GLAutoDrawable} implementations, when automatic buffer swapping
      is enabled (as is the default), this method is called
      automatically and should not be called by the end user. */
  public void swapBuffers() throws GLException;

  /** Fetches the {@link GLCapabilities} corresponding to the chosen
      OpenGL capabilities (pixel format / visual) for this drawable.
      Some drawables, in particular on-screen drawables, may be
      created lazily; null is returned if the drawable is not
      currently created or if its pixel format has not been set yet.
      On some platforms, the pixel format is not directly associated
      with the drawable; a best attempt is made to return a reasonable
      value in this case. */
  public GLCapabilities getChosenGLCapabilities();
}
