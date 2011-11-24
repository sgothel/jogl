/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
 
package javax.media.nativewindow;

/** Provides low-level information required for
    hardware-accelerated rendering using a surface in a platform-independent manner.<P>

    A NativeSurface created for a particular on- or offscreen component is
    expected to have the same lifetime as that component. As long as
    the component is alive and realized/visible, NativeSurface must be able
    provide information such as the surface handle while it is locked.<P>
*/
public interface NativeSurface extends SurfaceUpdatedListener {
  /** Unlocked state */
  public static final int LOCK_SURFACE_UNLOCKED = 0;

  /** Returned by {@link #lockSurface()} if the surface is not ready to be locked. */
  public static final int LOCK_SURFACE_NOT_READY = 1;

  /** Returned by {@link #lockSurface()} if the surface is locked, but has changed. */
  public static final int LOCK_SURFACE_CHANGED = 2;

  /** Returned by {@link #lockSurface()} if the surface is locked, and is unchanged. */
  public static final int LOCK_SUCCESS = 3;

  /**
   * Lock the surface of this native window<P>
   *
   * The surface handle, see {@link #lockSurface()}, <br>
   * shall be valid after a successfull call,
   * ie return a value other than {@link #LOCK_SURFACE_NOT_READY}.<P>
   *
   * This call is blocking until the surface has been locked
   * or a timeout is reached. The latter will throw a runtime exception. <P>
   *
   * This call allows recursion from the same thread.<P>
   *
   * The implementation may want to aquire the 
   * application level {@link com.jogamp.common.util.locks.RecursiveLock}
   * first before proceeding with a native surface lock. <P>
   *
   * The implementation shall also invoke {@link AbstractGraphicsDevice#lock()}
   * for the initial lock (recursive count zero).<P>
   *
   * @return {@link #LOCK_SUCCESS}, {@link #LOCK_SURFACE_CHANGED} or {@link #LOCK_SURFACE_NOT_READY}.
   *
   * @throws RuntimeException after timeout when waiting for the surface lock
   *
   * @see com.jogamp.common.util.locks.RecursiveLock
   */
  public int lockSurface();

  /**
   * Unlock the surface of this native window
   *
   * Shall not modify the surface handle, see {@link #lockSurface()} <P>
   *
   * The implementation shall also invoke {@link AbstractGraphicsDevice#unlock()}
   * for the final unlock (recursive count zero).<P>
   *
   * @throws RuntimeException if surface is not locked
   *
   * @see #lockSurface
   * @see com.jogamp.common.util.locks.RecursiveLock
   */
  public void unlockSurface() throws NativeWindowException ;

  /**
   * Return if surface is locked by another thread, ie not the current one
   */
  public boolean isSurfaceLockedByOtherThread();

  /**
   * Return if surface is locked
   */
  public boolean isSurfaceLocked();

  /**
   * Return the locking owner's Thread, or null if not locked.
   */
  public Thread getSurfaceLockOwner();

  /**
   * Provide a mechanism to utilize custom (pre-) swap surface
   * code. This method is called before the render toolkit (e.g. JOGL) 
   * swaps the buffer/surface. The implementation may itself apply the swapping,
   * in which case true shall be returned.
   *
   * @return true if this method completed swapping the surface,
   *         otherwise false, in which case eg the GLDrawable 
   *         implementation has to swap the code.
   */
  public boolean surfaceSwap();

  /** Appends the given {@link SurfaceUpdatedListener} to the end of the list. */
  public void addSurfaceUpdatedListener(SurfaceUpdatedListener l);

  /**
   * Inserts the given {@link SurfaceUpdatedListener} at the
   * specified position in the list.<br>
   *
   * @param index Position where the listener will be inserted.
   * Should be within (0 <= index && index <= size()).
   * An index value of -1 is interpreted as the end of the list, size().
   * @param l The listener object to be inserted
   * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
   */
  public void addSurfaceUpdatedListener(int index, SurfaceUpdatedListener l) throws IndexOutOfBoundsException;

  /** Remove the specified {@link SurfaceUpdatedListener} from the list. */
  public void removeSurfaceUpdatedListener(SurfaceUpdatedListener l);
  
  /**
   * Returns the handle to the surface for this NativeSurface. <P>
   * 
   * The surface handle should be set/update by {@link #lockSurface()},
   * where {@link #unlockSurface()} is not allowed to modify it.
   * After {@link #unlockSurface()} it is no more guaranteed 
   * that the surface handle is still valid.
   *
   * The surface handle shall reflect the platform one
   * for all drawable surface operations, e.g. opengl, swap-buffer. <P>
   *
   * On X11 this returns an entity of type Window,
   * since there is no differentiation of surface and window there. <BR>
   * On Microsoft Windows this returns an entity of type HDC.
   */
  public long getSurfaceHandle();

  /**
   * Returns the width of the client area excluding insets (window decorations).
   * @return width of the client area
   */
  public int getWidth();

  /**
   * Returns the height of the client area excluding insets (window decorations).
   * @return height of the client area
   */
  public int getHeight();

  /**
   * Returns the graphics configuration corresponding to this window.
   * <p>
   * In case the implementation utilizes a delegation pattern to wrap abstract toolkits,
   * this method shall return the native {@link AbstractGraphicsConfiguration} via {@link AbstractGraphicsConfiguration#getNativeGraphicsConfiguration()}.
   * </p>
   * @see AbstractGraphicsConfiguration#getNativeGraphicsConfiguration()
   * @see javax.media.nativewindow.GraphicsConfigurationFactory#chooseGraphicsConfiguration(Capabilities, CapabilitiesChooser, AbstractGraphicsScreen)
   */
  public AbstractGraphicsConfiguration getGraphicsConfiguration();

  /**
   * Convenience: Get display handle from 
   *   AbstractGraphicsConfiguration . AbstractGraphicsScreen . AbstractGraphicsDevice
   */
  public long getDisplayHandle();

  /**
   * Convenience: Get display handle from 
   *   AbstractGraphicsConfiguration . AbstractGraphicsScreen
   */
  public int  getScreenIndex();
  
}

