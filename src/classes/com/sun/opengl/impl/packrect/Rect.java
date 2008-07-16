/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.opengl.impl.packrect;

/** Represents a rectangular region on the backing store. The edges of
    the rectangle are the infinitely thin region between adjacent
    pixels on the screen. The origin of the rectangle is its
    upper-left corner. It is inclusive of the pixels on the top and
    left edges and exclusive of the pixels on the bottom and right
    edges. For example, a rect at position (0, 0) and of size (1, 1)
    would include only the pixel at (0, 0). <P>

    Negative coordinates and sizes are not supported, since they make
    no sense in the context of the packer, which deals only with
    positively sized regions. <P>

    This class contains a user data field for efficient hookup to
    external data structures as well as enough other hooks to
    efficiently plug into the rectangle packer. */

public class Rect {
  private int x;
  private int y;
  private int w;
  private int h;

  // The level we're currently installed in in the parent
  // RectanglePacker, or null if not hooked in to the table yet
  private Level level;

  // The user's object this rectangle represents.
  private Object userData;

  // Used transiently during re-layout of the backing store (when
  // there is no room left due either to fragmentation or just being
  // out of space)
  private Rect nextLocation;
  
  public Rect() {
    this(null);
  }

  public Rect(Object userData) {
    this(0, 0, 0, 0, userData);
  }

  public Rect(int x, int y, int w, int h, Object userData) {
    setPosition(x, y);
    setSize(w, h);
    setUserData(userData);
  }

  public int x() { return x; }
  public int y() { return y; }
  public int w() { return w; }
  public int h() { return h; }
  public Object getUserData() { return userData; }
  public Rect getNextLocation() { return nextLocation; }

  public void setPosition(int x, int y) {
    if (x < 0)
      throw new IllegalArgumentException("Negative x");
    if (y < 0)
      throw new IllegalArgumentException("Negative y");
    this.x = x;
    this.y = y;
  }

  public void setSize(int w, int h) throws IllegalArgumentException {
    if (w < 0)
      throw new IllegalArgumentException("Negative width");
    if (h < 0)
      throw new IllegalArgumentException("Negative height");
    this.w = w;
    this.h = h;
  }

  public void setUserData(Object obj) { userData = obj; }
  public void setNextLocation(Rect nextLocation) { this.nextLocation = nextLocation; }

  // Helpers for computations.

  /** Returns the maximum x-coordinate contained within this
      rectangle. Note that this returns a different result than Java
      2D's rectangles; for a rectangle of position (0, 0) and size (1,
      1) this will return 0, not 1. Returns -1 if the width of this
      rectangle is 0. */
  public int maxX() {
    if (w() < 1)
      return -1;
    return x() + w() - 1;
  }

  /** Returns the maximum y-coordinate contained within this
      rectangle. Note that this returns a different result than Java
      2D's rectangles; for a rectangle of position (0, 0) and size (1,
      1) this will return 0, not 1. Returns -1 if the height of this
      rectangle is 0. */
  public int maxY() {
    if (h() < 1)
      return -1;
    return y() + h() - 1;
  }

  public boolean canContain(Rect other) {
    return (w() >= other.w() &&
            h() >= other.h());
  }

  public String toString() {
    return "[Rect x: " + x() + " y: " + y() + " w: " + w() + " h: " + h() + "]";
  }

  // Unclear whether it's a good idea to override hashCode and equals
  // for these objects
  /*
  public boolean equals(Object other) {
    if (other == null || (!(other instanceof Rect))) {
      return false;
    }

    Rect r = (Rect) other;
    return (this.x() == r.x() &&
            this.y() == r.y() &&
            this.w() == r.w() &&
            this.h() == r.h());
  }

  public int hashCode() {
    return (x + y * 13 + w * 17 + h * 23);
  }
  */
}
