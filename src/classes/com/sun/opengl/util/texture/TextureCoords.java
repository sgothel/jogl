/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.sun.opengl.util.texture;

/** Specifies texture coordinates for a rectangular area of a
    texture. Note that some textures are inherently flipped vertically
    from OpenGL's standard coordinate system. This class takes care of
    this vertical flip so that the "bottom" and "top" coordinates may
    sometimes be reversed. From the point of view of code rendering
    textured polygons, it can always map the bottom and left texture
    coordinates from the TextureCoords to the lower left point of the
    textured polygon and achieve correct results. */

public class TextureCoords {
  // These represent the lower-left point
  private float left;
  private float bottom;
  // These represent the upper-right point
  private float right;
  private float top;

  public TextureCoords(float left, float bottom,
                       float right, float top) {
    this.left = left;
    this.bottom = bottom;
    this.right = right;
    this.top = top;
  }

  /** Returns the leftmost (x) texture coordinate of this
      rectangle. */
  public float left() { return left; }

  /** Returns the rightmost (x) texture coordinate of this
      rectangle. */
  public float right() { return right; }

  /** Returns the bottommost (y) texture coordinate of this
      rectangle. */
  public float bottom() { return bottom; }

  /** Returns the topmost (y) texture coordinate of this
      rectangle. */
  public float top() { return top; }
}
