/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
package com.jogamp.nativewindow;

import com.jogamp.nativewindow.util.PixelRectangle;
import com.jogamp.nativewindow.util.PointImmutable;

import com.jogamp.common.util.locks.RecursiveLock;

/**
 * Interface specifying the offscreen layer surface protocol.
 */
public interface OffscreenLayerSurface {
  /**
   * Attach the offscreen layer to this offscreen layer surface.
   * <p>
   * Implementation may realize all required resources at this point.
   * </p>
   *
   * @see #isOffscreenLayerSurfaceEnabled()
   * @throws NativeWindowException if {@link #isOffscreenLayerSurfaceEnabled()} == false
   */
  public void attachSurfaceLayer(final long layerHandle) throws NativeWindowException;

  /**
   * Detaches a previously attached offscreen layer from this offscreen layer surface.
   * @see #attachSurfaceLayer(long)
   * @see #isOffscreenLayerSurfaceEnabled()
   * @throws NativeWindowException if {@link #isOffscreenLayerSurfaceEnabled()} == false
   *                               or no surface layer is attached.
   */
  public void detachSurfaceLayer() throws NativeWindowException;

  /** Returns the attached surface layer or null if none is attached. */
  public long getAttachedSurfaceLayer();

  /** Returns true if a surface layer is attached, otherwise false. */
  public boolean isSurfaceLayerAttached();

  /** Sets the capabilities of this instance, allowing upstream API's to refine it, i.e. OpenGL related settings. */
  public void setChosenCapabilities(CapabilitiesImmutable caps);

  /** Returns the recursive lock object of this surface, which synchronizes multithreaded access. */
  public RecursiveLock getLock();

  /**
   * Optional method setting cursor in the corresponding on-screen surface/window, if exists.
   *
   * @param pixelrect cursor pixels, maybe null for default cursor
   * @param hotSpot maybe null for default cursor
   * @return true if successful, i.e. on-screen surface/window w/ cursor capabilities exists. Otherwise false.
   */
  public boolean setCursor(PixelRectangle pixelrect, PointImmutable hotSpot);

  /**
   * Optional method hiding the cursor in the corresponding on-screen surface/window, if exists.
   *
   * @return true if successful, i.e. on-screen surface/window w/ cursor capabilities exists. Otherwise false.
   */
  public boolean hideCursor();
}
