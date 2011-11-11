/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.NativeSurface;


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
   * underlying window has been created and can be drawn into. End
   * users do not need to call this method; it is not necessary to
   * call <code>setRealized</code> on a GLCanvas, a GLJPanel, or a
   * GLPbuffer, as these perform the appropriate calls on their
   * underlying GLDrawables internally.
   *
   * <P>
   *
   * Developers implementing new OpenGL components for various window
   * toolkits need to call this method against GLDrawables obtained
   * from the GLDrawableFactory via the {@link
   * GLDrawableFactory#getGLDrawable
   * GLDrawableFactory.getGLDrawable()} method. It must typically be
   * called with an argument of <code>true</code> when the component
   * associated with the GLDrawable is realized and with an argument
   * of <code>false</code> just before the component is unrealized.
   * For the AWT, this means calling <code>setRealized(true)</code> in
   * the <code>addNotify</code> method and with an argument of
   * <code>false</code> in the <code>removeNotify</code> method.
   *
   * <P>
   *
   * <code>GLDrawable</code> implementations should handle multiple
   * cycles of <code>setRealized(true)</code> /
   * <code>setRealized(false)</code> calls. Most, if not all, Java
   * window toolkits have a persistent object associated with a given
   * component, regardless of whether that component is currently
   * realized. The <CODE>GLDrawable</CODE> object associated with a
   * particular component is intended to be similarly persistent. A
   * <CODE>GLDrawable</CODE> is intended to be created for a given
   * component when it is constructed and live as long as that
   * component. <code>setRealized</code> allows the
   * <code>GLDrawable</code> to re-initialize and destroy any
   * associated resources as the component becomes realized and
   * unrealized, respectively.
   *
   * <P>
   *
   * With an argument of <code>true</code>, 
   * the minimum implementation shall call 
   * {@link NativeSurface#lockSurface() NativeSurface's lockSurface()} and if successfull:
   * <ul>
   *    <li> Update the {@link GLCapabilities}, which are associated with 
   *         the attached {@link NativeSurface}'s {@link AbstractGraphicsConfiguration}.</li>
   *    <li> Release the lock with {@link NativeSurface#unlockSurface() NativeSurface's unlockSurface()}.</li>
   * </ul><br>
   * This is important since {@link NativeSurface#lockSurface() NativeSurface's lockSurface()}
   * ensures resolving the window/surface handles, and the drawable's {@link GLCapabilities}
   * might have changed.
   *
   * <P>
   *
   * Calling this method has no other effects. For example, if
   * <code>removeNotify</code> is called on a Canvas implementation
   * for which a GLDrawable has been created, it is also necessary to
   * destroy all OpenGL contexts associated with that GLDrawable. This
   * is not done automatically by the implementation.
   */
  public void setRealized(boolean realized);

  /** @return true if this drawable is realized, otherwise false */
  public boolean isRealized();

  /** Returns the current width of this GLDrawable. */
  public int getWidth();

  /** Returns the current height of this GLDrawable. */
  public int getHeight();

  /** Swaps the front and back buffers of this drawable. For {@link
      GLAutoDrawable} implementations, when automatic buffer swapping
      is enabled (as is the default), this method is called
      automatically and should not be called by the end user. */
  public void swapBuffers() throws GLException;

  /** Fetches the {@link GLCapabilitiesImmutable} corresponding to the chosen
      OpenGL capabilities (pixel format / visual / GLProfile) for this drawable.<br>
      On some platforms, the pixel format is not directly associated
      with the drawable; a best attempt is made to return a reasonable
      value in this case. <br>
      This object shall be directly associated to the attached {@link NativeSurface}'s 
      {@link AbstractGraphicsConfiguration}, and if changes are necessary,
      they should reflect those as well.
      @return A copy of the queried object.
    */
  public GLCapabilitiesImmutable getChosenGLCapabilities();

  /** Fetches the {@link GLProfile} for this drawable.
      Returns the GLProfile object, no copy.
    */
  public GLProfile getGLProfile();

  /**
   * Returns the underlying native surface which surface handle 
   * represents this OpenGL drawable's native resource.
   * 
   * @see #getHandle()
   */
  public NativeSurface getNativeSurface();

  /** 
   * This is the GL/Windowing drawable handle.<br>
   * It is usually the {@link javax.media.nativewindow.NativeSurface#getSurfaceHandle()},
   * ie the native surface handle of the underlying windowing toolkit.<br>
   * However, on X11/GLX this reflects a GLXDrawable, which represents a GLXWindow, GLXPixmap, or GLXPbuffer.<br>
   * On EGL, this represents the EGLSurface.<br>
   */
  public long getHandle();

  public GLDrawableFactory getFactory();

  public String toString();
}
