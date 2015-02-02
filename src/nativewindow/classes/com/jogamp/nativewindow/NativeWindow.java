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

package com.jogamp.nativewindow;

import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.nativewindow.util.Point;

/**
 * Extend the {@link NativeSurface} interface with windowing
 * information such as {@link #getWindowHandle() window-handle},
 * {@link #getWidth() window-size} and {@link #getX() window-position}.
 * <p>
 * All values of this interface are represented in window units, if not stated otherwise.
 * See {@link NativeSurface}.
 * </p>
 *
 * <a name="coordinateSystem"><h5>Coordinate System</h5></a>
 * <p>
 *  <ul>
 *      <li>Abstract screen space has it's origin in the top-left corner, and may not be at 0/0.</li>
 *      <li>Window origin is in it's top-left corner, see {@link #getX()} and {@link #getY()}. </li>
 *      <li>Window client-area excludes {@link #getInsets() insets}, i.e. window decoration.</li>
 *      <li>Window origin is relative to it's parent window if exist, or the screen position (top-level).</li>
 *  </ul>
 * </p>
 * <p>
 * A window toolkit such as the AWT may either implement this interface
 * directly with one of its components, or provide and register an
 * implementation of {@link NativeWindowFactory NativeWindowFactory}
 * which can create NativeWindow objects for its components.
 * </p>
 */
public interface NativeWindow extends NativeSurface, NativeSurfaceHolder {

  /**
   * {@inheritDoc}
   * <p>
   * Returns this instance, which <i>is-a</i> {@link NativeSurface}.
   * </p>
   */
  @Override
  public NativeSurface getNativeSurface();

  /**
   * Destroys this window incl. releasing all related resources.
   */
  public void destroy();

  /**
   * @return The parent NativeWindow, or null if this NativeWindow is top level.
   */
  public NativeWindow getParent();

  /**
   * Returns the window handle for this NativeWindow. <P>
   *
   * The window handle shall reflect the platform one
   * for all window related operations, e.g. open, close, resize. <P>
   *
   * On X11 this returns an entity of type Window. <BR>
   * On Microsoft Windows this returns an entity of type HWND.
   */
  public long getWindowHandle();

  /**
   * Returns the insets defined as the width and height of the window decoration
   * on the left, right, top and bottom in window units.
   * <p>
   * Insets are zero if the window is undecorated, including child windows.
   * </p>
   *
   * <p>
   * Insets are available only after the native window has been created,
   * ie. the native window has been made visible.<br>
   *
   * The top-level window area's top-left corner is located at
   * <pre>
   *   {@link #getX()} - getInsets().{@link InsetsImmutable#getLeftWidth() getLeftWidth()}
   *   {@link #getY()} - getInsets().{@link InsetsImmutable#getTopHeight() getTopHeight()}
   * </pre>
   *
   * The top-level window size is
   * <pre>
   *   {@link #getWidth()}  + getInsets().{@link InsetsImmutable#getTotalWidth() getTotalWidth()}
   *   {@link #getHeight()} + getInsets().{@link InsetsImmutable#getTotalHeight() getTotalHeight()}
   * </pre>
   *
   * @return insets
   */
  public InsetsImmutable getInsets();

  /** Returns the current x position of this window, relative to it's parent. */

  /**
   * Returns the x position of the top-left corner
   * of the client area relative to it's parent in window units.
   * <p>
   * If no parent exist (top-level window), this coordinate equals the screen coordinate.
   * </p>
   * <p>
   * Since the position reflects the client area, it does not include the insets.
   * </p>
   * <p>
   * See <a href="#coordinateSystem"> Coordinate System</a>.
   * </p>
   * @see #getInsets()
   * @see #getLocationOnScreen(Point)
   */
  public int getX();

  /**
   * Returns the current y position of the top-left corner
   * of the client area relative to it's parent in window units.
   * <p>
   * If no parent exist (top-level window), this coordinate equals the screen coordinate.
   * </p>
   * <p>
   * Since the position reflects the client area, it does not include the insets.
   * </p>
   * <p>
   * See <a href="#coordinateSystem"> Coordinate System</a>.
   * </p>
   * @see #getInsets()
   * @see #getLocationOnScreen(Point)
   */
  public int getY();

  /**
   * Returns the width of the client area excluding insets (window decorations) in window units.
   * @return width of the client area in window units
   * @see NativeSurface#getSurfaceWidth()
   */
  public int getWidth();

  /**
   * Returns the height of the client area excluding insets (window decorations) in window units.
   * @return height of the client area in window units
   * @see NativeSurface#getSurfaceHeight()
   */
  public int getHeight();

  /**
   * Returns the window's top-left client-area position in the screen.
   * <p>
   * If {@link Point} is not <code>null</code>, it is translated about the resulting screen position
   * and returned.
   * </p>
   * <p>
   * See <a href="#coordinateSystem"> Coordinate System</a>.
   * </p>
   * <p>
   * Since the position reflects the client area, it does not include the insets.
   * </p>
   * @param point Optional {@link Point} storage.
   *              If not null, <code>null</code>, it is translated about the resulting screen position
   *              and returned.
   * @see #getX()
   * @see #getY()
   * @see #getInsets()
   */
  public Point getLocationOnScreen(Point point);

  /** Returns true if this native window owns the focus, otherwise false. */
  boolean hasFocus();

}
