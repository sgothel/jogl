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

package net.java.games.jogl;

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

/** Abstracts common functionality among the OpenGL components {@link
    GLCanvas} and {@link GLJPanel}. */

public interface GLDrawable extends ComponentEvents {
  /** Requests a new width and height for this GLDrawable. Not all
      drawables are able to respond to this request and may silently
      ignore it. */
  public void setSize(int width, int height);

  /** Returns the current width of this GLDrawable. */
  public int getWidth();

  /** Returns the current height of this GLDrawable. */
  public int getHeight();

  /** Returns the {@link GL} pipeline object this GLDrawable uses.  If
      this method is called outside of the {@link GLEventListener}'s
      callback methods (init, display, etc.) it may return null. Users
      should not rely on the identity of the returned GL object; for
      example, users should not maintain a hash table with the GL
      object as the key. Additionally, the GL object should not be
      cached in client code, but should be re-fetched from the
      GLDrawable at the beginning of each call to init, display,
      etc. */
  public GL getGL();

  /** Sets the {@link GL} pipeline object this GLDrawable uses. This
      should only be called from within the GLEventListener's callback
      methods, and usually only from within the init() method, in
      order to install a composable pipeline. See the JOGL demos for
      examples. */
  public void setGL(GL gl);

  /** Returns the {@link GLU} pipeline object this GLDrawable uses. */
  public GLU getGLU();

  /** Sets the {@link GLU} pipeline object this GLDrawable uses. */
  public void setGLU(GLU glu);

  /** Causes OpenGL rendering to be performed for this GLDrawable by
      calling {@link GLEventListener#display} for all registered
      {@link GLEventListener}s. Called automatically by the window
      system toolkit upon receiving a repaint() request. When used in
      conjunction with {@link
      net.java.games.jogl.GLDrawable#setRenderingThread}, this routine
      may be called manually by the application's main loop for higher
      performance and better control over the rendering process. It is
      legal to call another GLDrawable's display method from within
      {@link GLEventListener#display}. */
  public void display();

  /** Enables or disables automatic buffer swapping for this drawable.
      By default this property is set to true; when true, after all
      GLEventListeners have been called for a display() event, the
      front and back buffers are swapped, displaying the results of
      the render. When disabled, the user is responsible for calling
      {@link #swapBuffers} manually. */
  public void setAutoSwapBufferMode(boolean onOrOff);

  /** Indicates whether automatic buffer swapping is enabled for this
      drawable. See {@link #setAutoSwapBufferMode}. */
  public boolean getAutoSwapBufferMode();

  /** Swaps the front and back buffers of this drawable. When
      automatic buffer swapping is enabled (as is the default), it is
      not necessary to call this method and doing so may have
      undefined results. */
  public void swapBuffers();

  /** Indicates whether this drawable is capable of fabricating a
      subordinate offscreen drawable for advanced rendering techniques
      which require offscreen hardware-accelerated surfaces. Note that
      this method is only guaranteed to return a correct result once
      your GLEventListener's init() method has been called. */
  public boolean canCreateOffscreenDrawable();

  /** Creates a subordinate offscreen drawable (pbuffer) for this
      drawable. This routine should only be called if {@link
      #canCreateOffscreenDrawable} returns true. The passed
      capabilities are matched according to the platform-dependent
      pbuffer format selection algorithm, which currently can not be
      overridden. */
  public GLPbuffer createOffscreenDrawable(GLCapabilities capabilities,
                                           int initialWidth,
                                           int initialHeight);
}
