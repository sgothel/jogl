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

package javax.media.nativewindow;

import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.nativewindow.util.Point;

/** Extend the {@link NativeSurface} interface with windowing
    information such as window handle and position.<P>

    A window toolkit such as the AWT may either implement this interface
    directly with one of its components, or provide and register an
    implementation of {@link NativeWindowFactory NativeWindowFactory}
    which can create NativeWindow objects for its components. <P>
*/
public interface NativeWindow extends NativeSurface {
 
  /** 
   * destroys the window and releases
   * windowing related resources.
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
   * on the left, right, top and bottom.<br>
   * Insets are zero if the window is undecorated, including child windows.
   * 
   * <p>
   * Insets are available only after the native window has been created,
   * ie. the native window has been made visible.<br>
   *   
   * The top-level window area's top-left corner is located at
   * <pre>
   *   getX() - getInsets().{@link InsetsImmutable#getLeftWidth() getLeftWidth()}
   *   getY() - getInsets().{@link InsetsImmutable#getTopHeight() getTopHeight()}
   * </pre> 
   * 
   * The top-level window size is
   * <pre>
   *   getWidth()  + getInsets().{@link InsetsImmutable#getTotalWidth() getTotalWidth()} 
   *   getHeight() + getInsets().{@link InsetsImmutable#getTotalHeight() getTotalHeight()}
   * </pre> 
   * 
   * @return insets
   */
  public InsetsImmutable getInsets();
  
  /** Returns the current x position of this window, relative to it's parent. */
  
  /** 
   * @return the current x position of the top-left corner
   *         of the client area relative to it's parent. 
   *         Since the position reflects the client area, it does not include the insets.
   * @see #getInsets()
   */
  public int getX();

  /** 
   * @return the current y position of the top-left corner
   *         of the client area relative to it's parent. 
   *         Since the position reflects the client area, it does not include the insets.
   * @see #getInsets()
   */
  public int getY();

  /** 
   * Returns the current position of the top-left corner 
   * of the client area in screen coordinates.
   * <p>
   * Since the position reflects the client area, it does not include the insets.
   * </p> 
   * @param point if not null,
   *        {@link javax.media.nativewindow.util.Point#translate(javax.media.nativewindow.util.Point)}
   *        the passed {@link javax.media.nativewindow.util.Point} by this location on the screen and return it.
   * @return either the passed non null translated point by the screen location of this NativeWindow,
   *         or a new instance with the screen location of this NativeWindow.
   */
  public Point getLocationOnScreen(Point point);
  
  /** Returns true if this native window owns the focus, otherwise false. */
  boolean hasFocus();
  
}
