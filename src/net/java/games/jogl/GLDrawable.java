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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

import java.awt.Dimension;

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
  /** Adds a {@link GLEventListener} to this drawable. If multiple
      listeners are added to a given drawable, they are notified of
      events in an arbitrary order. */
  public void addGLEventListener(GLEventListener listener);

  /** Removes a {@link GLEventListener} from this drawable. Note that
      if this is done from within a particular drawable's {@link
      GLEventListener} handler (reshape, display, etc.) that it is not
      guaranteed that all other listeners will be evaluated properly
      during this update cycle. */
  public void removeGLEventListener(GLEventListener listener);

  /** Sets the size of this GLDrawable. */
  public void setSize(int width, int height);

  /** Sets the size of this GLDrawable. */
  public void setSize(Dimension d);

  /** Returns the size of this GLDrawable as a newly-created Dimension
      object. */
  public Dimension getSize();

  /** Stores the size of this GLDrawable into the user-provided
      Dimension object, returning that object. If the provided
      Dimension is null a new one will be allocated and returned. */
  public Dimension getSize(Dimension d);

  /** Returns the {@link GL} pipeline object this GLDrawable uses. */
  public GL getGL();

  /** Sets the {@link GL} pipeline object this GLDrawable uses. */
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

  /** <P> Changes this GLDrawable to allow OpenGL rendering only from
      the supplied thread, which must either be the current thread or
      null. Attempts by other threads to perform OpenGL operations
      like rendering or resizing the window will be ignored as long as
      the thread is set. Setting up the rendering thread is not
      required but enables the system to perform additional
      optimizations, in particular when the application requires
      control over the rendering loop. Before exiting,
      <code>setRenderingThread(null)</code> must be called or other
      threads will be unable to perform OpenGL rendering to this
      drawable. Throws {@link GLException} if the rendering thread for
      this drawable has been set and attempts are made to set or clear
      the rendering thread from another thread, or if the passed
      thread is not equal to the current thread or null. Also throws
      {@link GLException} if the current thread attempts to call
      <code>setRenderingThread</code> on more than one drawable. </P>
      
      <P> <B>NOTE:</B> Currently this routine is only advisory, which
      means that on some platforms the underlying optimizations are
      disabled and setting the rendering thread has no effect.
      Applications should not rely on setRenderingThread to prevent
      rendering from other threads. <P>

      @throws GLException if the rendering thread for this drawable has
      been set by another thread or if the passed thread is not equal
      to the current thread or null
  */
  public void setRenderingThread(Thread currentThreadOrNull) throws GLException;

  /** Returns the rendering thread for this drawable, or null if none
      has been set. */
  public Thread getRenderingThread();

  /** Disables automatic redraws of this drawable if possible. This is
      provided as an overriding mechanism for applications which
      perform animation on the drawable and for which the (currently
      advisory) {@link #setRenderingThread} does not provide strict
      enough guarantees. Its sole purpose is to avoid deadlocks that
      are unfortunately all too easy to run into when both animating a
      drawable from a given thread as well as having updates performed
      by the AWT event thread (repaints, etc.). When it is enabled,
      repaint requests driven by the AWT will not result in the OpenGL
      event listeners' display methods being called from the AWT
      thread, unless (as with GLJPanel) this is the only mechanism by
      which repaints are done. The necessity of this API may be
      rethought in a future release. Defaults to false. */
  public void setNoAutoRedrawMode(boolean noAutoRedraws);

  /** Returns whether automatic redraws are disabled for this
      drawable. Defaults to false. */
  public boolean getNoAutoRedrawMode();

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
