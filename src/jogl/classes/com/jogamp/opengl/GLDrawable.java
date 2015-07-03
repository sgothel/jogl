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

package com.jogamp.opengl;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeSurfaceHolder;


/** An abstraction for an OpenGL rendering target. A GLDrawable's
    primary functionality is to create OpenGL contexts which can be
    used to perform rendering. A GLDrawable does not automatically
    create an OpenGL context, but all implementations of {@link
    GLAutoDrawable} do so upon creation. */

public interface GLDrawable extends NativeSurfaceHolder {
  /**
   * Creates a new context for drawing to this drawable that will
   * optionally share buffer objects, textures and other server-side OpenGL
   * objects with the specified GLContext.
   * <p>
   * The GLContext <code>share</code> need not be associated with this
   * GLDrawable and may be null if sharing of display lists and other
   * objects is not desired. See the note in the overview
   * documentation
   * <a href="../../../overview-summary.html#SHARING">context sharing</a>
   * as well as {@link GLSharedContextSetter}.
   * </p>
   */
  public GLContext createContext(GLContext shareWith);

  /**
   * Indicates to GLDrawable implementations whether the
   * underlying {@link NativeSurface surface} has been created and can be drawn into.
   * <p>
   * If realized, the {@link #getHandle() drawable handle} may become
   * valid while it's {@link NativeSurface surface} is being {@link NativeSurface#lockSurface() locked}.
   * </p>
   * <p>
   * End users do not need to call this method; it is not necessary to
   * call <code>setRealized</code> on a {@link GLAutoDrawable}
   * as these perform the appropriate calls on their underlying GLDrawables internally.
   * </p>
   * <p>
   * Developers implementing new OpenGL components for various window
   * toolkits need to call this method against GLDrawables obtained
   * from the GLDrawableFactory via the
   * {@link GLDrawableFactory#createGLDrawable(NativeSurface)} method.
   * It must typically be
   * called with an argument of <code>true</code> when the component
   * associated with the GLDrawable is realized and with an argument
   * of <code>false</code> just before the component is unrealized.
   * For the AWT, this means calling <code>setRealized(true)</code> in
   * the <code>addNotify</code> method and with an argument of
   * <code>false</code> in the <code>removeNotify</code> method.
   * </p>
   * <p>
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
   * </p>
   * <p>
   * With an argument of <code>true</code>,
   * the minimum implementation shall call
   * {@link NativeSurface#lockSurface() NativeSurface's lockSurface()} and if successful:
   * <ul>
   *    <li> Update the {@link GLCapabilities}, which are associated with
   *         the attached {@link NativeSurface}'s {@link AbstractGraphicsConfiguration}.</li>
   *    <li> Release the lock with {@link NativeSurface#unlockSurface() NativeSurface's unlockSurface()}.</li>
   * </ul><br>
   * This is important since {@link NativeSurface#lockSurface() NativeSurface's lockSurface()}
   * ensures resolving the window/surface handles, and the drawable's {@link GLCapabilities}
   * might have changed.
   * </p>
   * <p>
   * Calling this method has no other effects. For example, if
   * <code>removeNotify</code> is called on a Canvas implementation
   * for which a GLDrawable has been created, it is also necessary to
   * destroy all OpenGL contexts associated with that GLDrawable. This
   * is not done automatically by the implementation.
   * </p>
   * @see #isRealized()
   * @see #getHandle()
   * @see NativeSurface#lockSurface()
   */
  public void setRealized(boolean realized);

  /**
   * Returns <code>true</code> if this drawable is realized, otherwise <code>true</code>.
   * <p>
   * A drawable can be realized and unrealized via {@link #setRealized(boolean)}.
   * </p>
   * @see #setRealized(boolean)
   */
  public boolean isRealized();

  /**
   * Returns the width of this {@link GLDrawable}'s {@link #getNativeSurface() surface} client area in pixel units.
   * @see NativeSurface#getSurfaceWidth()
   */
  public int getSurfaceWidth();

  /**
   * Returns the height of this {@link GLDrawable}'s {@link #getNativeSurface() surface} client area in pixel units.
   * @see NativeSurface#getSurfaceHeight()
   */
  public int getSurfaceHeight();

  /**
   * Returns <code>true</code> if the drawable is rendered in
   * OpenGL's coordinate system, <i>origin at bottom left</i>.
   * Otherwise returns <code>false</code>, i.e. <i>origin at top left</i>.
   * <p>
   * Default impl. is <code>true</code>, i.e. OpenGL coordinate system.
   * </p>
   * <p>
   * Currently only MS-Windows bitmap offscreen drawable uses a non OpenGL orientation and hence returns <code>false</code>.<br/>
   * This removes the need of a vertical flip when used in AWT or Windows applications.
   * </p>
   */
  public boolean isGLOriented();

  /** Swaps the front and back buffers of this drawable. For {@link
      GLAutoDrawable} implementations, when automatic buffer swapping
      is enabled (as is the default), this method is called
      automatically and should not be called by the end user. */
  public void swapBuffers() throws GLException;

  /** Fetches the {@link GLCapabilitiesImmutable} corresponding to the chosen
      OpenGL capabilities (pixel format / visual / GLProfile) for this drawable.
      <p>
      This query only returns the chosen capabilities if {@link #isRealized()}.
      </p>
      <p>
      On some platforms, the pixel format is not directly associated
      with the drawable; a best attempt is made to return a reasonable
      value in this case.
      </p>
      <p>
      This object shall be directly associated to the attached {@link NativeSurface}'s
      {@link AbstractGraphicsConfiguration}, and if changes are necessary,
      they should reflect those as well.
      </p>
      @return The immutable queried instance.
      @see #getRequestedGLCapabilities()
    */
  public GLCapabilitiesImmutable getChosenGLCapabilities();

  /** Fetches the {@link GLCapabilitiesImmutable} corresponding to the user requested
      OpenGL capabilities (pixel format / visual / GLProfile) for this drawable.
      <p>
      If {@link #isRealized() realized}, {@link #getChosenGLCapabilities() the chosen capabilities}
      reflect the actual selected OpenGL capabilities.
      </p>
      @return The immutable queried instance.
      @see #getChosenGLCapabilities()
      @since 2.2
    */
  public GLCapabilitiesImmutable getRequestedGLCapabilities();

  /** Fetches the {@link GLProfile} for this drawable.
      Returns the GLProfile object, no copy.
    */
  public GLProfile getGLProfile();

  /**
   * {@inheritDoc}
   * <p>
   * Returns the underlying {@link NativeSurface} which {@link NativeSurface#getSurfaceHandle() native handle}
   * represents this OpenGL drawable's native resource.
   * </p>
   *
   * @see #getHandle()
   */
  @Override
  public NativeSurface getNativeSurface();

  /**
   * Returns the GL drawable handle,
   * guaranteed to be valid after {@link #setRealized(boolean) realization}
   * <i>and</i> while it's {@link NativeSurface surface} is being {@link NativeSurface#lockSurface() locked}.
   * <p>
   * It is usually identical to the underlying windowing toolkit {@link NativeSurface surface}'s
   * {@link com.jogamp.nativewindow.NativeSurface#getSurfaceHandle() handle}
   * or an intermediate layer to suite GL, e.g. an EGL surface.
   * </p>
   * <p>
   * On EGL it is represented by the EGLSurface.<br>
   * On X11/GLX it is represented by either the Window XID, GLXPixmap, or GLXPbuffer.<br>
   * On Windows it is represented by the HDC, which may change with each {@link NativeSurface#lockSurface()}.<br>
   * </p>
   * @see #setRealized(boolean)
   * @see NativeSurface#lockSurface()
   * @see NativeSurface#unlockSurface()
   */
  public long getHandle();

  /** Return the {@link GLDrawableFactory} being used to create this instance. */
  public GLDrawableFactory getFactory();

  @Override
  public String toString();
}
