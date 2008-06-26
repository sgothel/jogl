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

public interface NativeWindow {
  public static final int LOCK_NOT_SUPPORTED = 0;
  public static final int LOCK_SURFACE_NOT_READY = 1;
  public static final int LOCK_SURFACE_CHANGED = 2;
  public static final int LOCK_SUCCESS = 3;

  /**
   * Lock this surface
   */
  public int lockSurface() throws NativeWindowException ;

  /**
   * Unlock this surface
   */
  public void unlockSurface();
  public boolean isSurfaceLocked();

  /** 
   * render all native window information invalid,
   * as if the native window was destroyed
   */
  public void invalidate();

  /**
   * Lifetime: locked state
   */
  public long getDisplayHandle();
  public long getScreenHandle();

  /**
   * Returns the window handle for this NativeWindow. 
   *
   * The window handle shall reflect the platform one 
   * for all window related operations, e.g. open, close, resize.
   *
   * On X11 this returns an entity of type Window. 
   * On Microsoft Windows this returns an entity of type HWND. 
   */
  public long getWindowHandle() throws NativeWindowException;

  /**
   * Returns the handle to the surface for this NativeWindow. 
   * 
   * The surface handle shall reflect the platform one
   * for all drawable surface operations, e.g. opengl, swap-buffer.
   *
   * On X11 this returns an entity of type Window,
   * since there is no differentiation of surface and window there.
   * On Microsoft Windows this returns an entity of type HDC.
   */
  public long getSurfaceHandle() throws NativeWindowException;

  /**
   * Lifetime: after 1st lock, until invalidation
   */
  public long getVisualID();
  public int  getScreenIndex();

  public void setSize(int width, int height);
  public void setPosition(int x, int y);
  public int getWidth();
  public int getHeight();
  public int getX();
  public int getY();

  public void setVisible(boolean visible);
  public boolean setFullscreen(boolean fullscreen);
  public boolean isVisible();
  public boolean isFullscreen();
}
