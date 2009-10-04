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

package javax.media.nativewindow;

/** Provides the low-level information required for
    hardware-accelerated rendering in a platform-independent manner. A
    window toolkit such as the AWT may either implement this interface
    directly with one of its components, or provide and register an
    implementation of {@link NativeWindowFactory NativeWindowFactory}
    which can create NativeWindow objects for its components. <P>

    A NativeWindow created for a particular on-screen component is
    expected to have the same lifetime as that component. As long as
    the component is alive, the NativeWindow must be able to control
    it, and any time it is visible and locked, provide information
    such as the window handle.
*/
public interface NativeWindow extends SurfaceUpdatedListener {
  /** Returned by {@link #lockSurface()} if the surface is not ready to be locked. */
  public static final int LOCK_SURFACE_NOT_READY = 1;

  /** Returned by {@link #lockSurface()} if the surface is locked, but has changed. */
  public static final int LOCK_SURFACE_CHANGED = 2;

  /** Returned by {@link #lockSurface()} if the surface is locked, and is unchanged. */
  public static final int LOCK_SUCCESS = 3;

  /**
   * Lock the surface of this native window<P>
   *
   * The window handle, see {@link #getWindowHandle()},
   * and the surface handle, see {@link #lockSurface()}, <br>
   * shall be set and be valid after a successfull call,
   * ie a return value other than {@link #LOCK_SURFACE_NOT_READY}.<P>
   *
   * The semantics of the underlying native locked resource
   * may be related to the {@link ToolkitLock} one. Hence it is 
   * important that implementation of both harmonize well.<br>
   * The implementation may want to aquire the {@link ToolkitLock}
   * first to become it's owner before proceeding with it's
   * actual surface lock. <P>
   *
   * @return {@link #LOCK_SUCCESS}, {@link #LOCK_SURFACE_CHANGED} or {@link #LOCK_SURFACE_NOT_READY}.
   *
   * @throws NativeWindowException if surface is already locked
   *
   * @see ToolkitLock
   */
  public int lockSurface() throws NativeWindowException ;

  /**
   * Unlock the surface of this native window
   *
   * Shall not modify the window handle, see {@link #getWindowHandle()},
   * or the surface handle, see {@link #lockSurface()} <P>
   *
   * @throws NativeWindowException if surface is not locked
   *
   * @see #lockSurface
   * @see ToolkitLock
   */
  public void unlockSurface() throws NativeWindowException ;

  /**
   * Return if surface is locked
   */
  public boolean isSurfaceLocked();

  /**
   * Return the lock-exception, or null if not locked.
   *
   * The lock-exception is created at {@link #lockSurface()}
   * and hence holds the locker's call stack.
   */
  public Exception getLockedStack();

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

  /** 
   * render all native window information invalid,
   * as if the native window was destroyed
   *
   * @see #destroy
   */
  public void invalidate();

  /** 
   * destroys the window and releases
   * windowing related resources.
   */
  public void destroy();

  /**
   * Returns the window handle for this NativeWindow. <P>
   *
   * The window handle should be set/update by {@link #lockSurface()},
   * where {@link #unlockSurface()} is not allowed to modify it.<br>
   * After {@link #unlockSurface()} it is no more guaranteed 
   * that the window handle is still valid.<p>
   *
   * The window handle shall reflect the platform one 
   * for all window related operations, e.g. open, close, resize. <P>
   *
   * On X11 this returns an entity of type Window. <BR>
   * On Microsoft Windows this returns an entity of type HWND. 
   */
  public long getWindowHandle();

  /**
   * Returns the handle to the surface for this NativeWindow. <P>
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

  /** Returns the current width of this window. */
  public int getWidth();

  /** Returns the current height of this window. */
  public int getHeight();

  /**
   * Returns the graphics configuration corresponding to this window.
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
