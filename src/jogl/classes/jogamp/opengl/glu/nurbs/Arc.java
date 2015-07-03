package jogamp.opengl.glu.nurbs;

/*
** License Applicability. Except to the extent portions of this file are
** made subject to an alternative license as permitted in the SGI Free
** Software License B, Version 2.0 (the "License"), the contents of this
** file are subject only to the provisions of the License. You may not use
** this file except in compliance with the License. You may obtain a copy
** of the License at Silicon Graphics, Inc., attn: Legal Services, 1600
** Amphitheatre Parkway, Mountain View, CA 94043-1351, or at:
**
** http://oss.sgi.com/projects/FreeB
**
** Note that, as provided in the License, the Software is distributed on an
** "AS IS" basis, with ALL EXPRESS AND IMPLIED WARRANTIES AND CONDITIONS
** DISCLAIMED, INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTIES AND
** CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A
** PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
**
** Original Code. The Original Code is: OpenGL Sample Implementation,
** Version 1.2.1, released January 26, 2000, developed by Silicon Graphics,
** Inc. The Original Code is Copyright (c) 1991-2000 Silicon Graphics, Inc.
** Copyright in any portions created by third parties is as indicated
** elsewhere herein. All Rights Reserved.
**
** Additional Notice Provisions: The application programming interfaces
** established by SGI in conjunction with the Original Code are The
** OpenGL(R) Graphics System: A Specification (Version 1.2.1), released
** April 1, 1999; The OpenGL(R) Graphics System Utility Library (Version
** 1.3), released November 4, 1998; and OpenGL(R) Graphics with the X
** Window System(R) (Version 1.3), released October 19, 1998. This software
** was created using the OpenGL(R) version 1.2.1 Sample Implementation
** published by SGI, but has not been independently verified as being
** compliant with the OpenGL(R) version 1.2.1 Specification.
*/

/**
 * Trimming arc
 * @author Tomas Hrasky
 *
 */
public class Arc {
  /**
   * Corresponding picewise-linear arc
   */
  public PwlArc pwlArc;

  /**
   * Arc type
   */
  private long type;

  /**
   * Arc link in linked list
   */
  public Arc link;

  /**
   * Previous arc
   */
  Arc prev;

  /**
   * Next arc
   */
  Arc next;

  /**
   * Corresponding berizer type arc
   */
  private final BezierArc bezierArc;

  /**
   * Makes new arc at specified side
   *
   * @param side
   *            which side doeas this arc form
   */
  public Arc(final int side) {
    bezierArc = null;
    pwlArc = null;
    type = 0;
    setside(side);
    // nuid=_nuid
  }

  /**
   * Sets side the arc is at
   *
   * @param side
   *            arc side
   */
  private void setside(final int side) {
    // DONE
    clearside();
    type |= side << 8;
  }

  /**
   * Unsets side
   */
  private void clearside() {
    // DONE
    type &= ~(0x7 << 8);
  }

  // this one replaces enum arc_side
  /**
   * Side not specified
   */
  public static final int ARC_NONE = 0;

  /**
   * Arc on right
   */
  public static final int ARC_RIGHT = 1;

  /**
   * Arc on top
   */
  public static final int ARC_TOP = 2;

  /**
   * Arc on left
   */
  public static final int ARC_LEFT = 3;

  /**
   * Arc on bottom
   */
  public static final int ARC_BOTTOM = 4;

  /**
   * Bezier type flag
   */
  private static final long BEZIER_TAG = 1 << 13;

  /**
   * Arc type flag
   */
  private static final long ARC_TAG = 1 << 3;

  /**
   * Tail type tag
   */
  private static final long TAIL_TAG = 1 << 6;

  /**
   * Appends arc to the list
   *
   * @param jarc
   *            arc to be append
   * @return this
   */
  public Arc append(final Arc jarc) {
    // DONE
    if (jarc != null) {
      next = jarc.next;
      prev = jarc;
      next.prev = this;
      prev.next = this;
    } else {
      next = this;
      prev = this;
    }

    return this;
  }

  /**
   * Unused
   *
   * @return true
   */
  public boolean check() {
    return true;
  }

  /**
   * Sets bezier type flag
   */
  public void setbezier() {
    // DONE
    type |= BEZIER_TAG;

  }

  /**
   * Returns tail of linked list coords
   *
   * @return tail coords
   */
  public float[] tail() {
    // DONE
    return pwlArc.pts[0].param;
  }

  /**
   * Returns head of linked list coords
   *
   * @return head coords
   */
  public float[] head() {
    // DONE
    return next.pwlArc.pts[0].param;
  }

  /**
   * Returns whether arc is marked with arc_tag
   *
   * @return is arc marked with arc_tag
   */
  public boolean ismarked() {
    // DONE
    return ((type & ARC_TAG) > 0) ? true : false;
  }

  /**
   * Cleans arc_tag flag
   */
  public void clearmark() {
    // DONE
    type &= (~ARC_TAG);
  }

  /**
   * Sets arc_tag flag
   */
  public void setmark() {
    // DONE
    type |= ARC_TAG;
  }

  /**
   * sets tail tag
   */
  public void setitail() {
    // DONE
    type |= TAIL_TAG;
  }

  /**
   * Returns whether arc is marked tail
   *
   * @return is tail
   */
  public boolean getitail() {
    return false;
  }

  /**
   * Unsets tail tag
   */
  public void clearitail() {
    // DONE
    type &= (~TAIL_TAG);
  }
}
