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
 * Knot vector used in curve specification
 *
 * @author Tomas Hrasky
 *
 */
public class Knotvector {

  /**
   * Tolerance used when comparing knots - when difference is smaller, knots
   * are considered equal
   */
  public static final float TOLERANCE = 1.0e-5f;

  /**
   * Maximum curve order
   */
  private static final int MAXORDER = 24;

  /**
   * Number of knots
   */
  int knotcount;

  /**
   * Number of control points' coordinates
   */
  int stride;

  /**
   * Curve order
   */
  int order;

  /**
   * Knots
   */
  float[] knotlist;

  /**
   * Makes new knotvector
   *
   * @param nknots
   *            number of knots
   * @param stride
   *            number of ctrl points' corrdinates
   * @param order
   *            curve order
   * @param knot
   *            knots
   */
  public Knotvector(final int nknots, final int stride, final int order, final float[] knot) {
    // DONE
    init(nknots, stride, order, knot);
  }

  /**
   * Initializes knotvector
   *
   * @param nknots
   *            number of knots
   * @param stride
   *            number of ctrl points' corrdinates
   * @param order
   *            curve order
   * @param knot
   *            knots
   */
  public void init(final int nknots, final int stride, final int order, final float[] knot) {
    // DONE
    this.knotcount = nknots;
    this.stride = stride;
    this.order = order;
    this.knotlist = new float[nknots];
    for (int i = 0; i < nknots; i++) {
      this.knotlist[i] = knot[i];
    }

  }

  /**
   * Validates knot vector parameters
   *
   * @return knot vector validity
   */
  public int validate() {
    int kindex = knotcount - 1;
    if (order < 1 || order > MAXORDER) {
      return 1;
    }
    if (knotcount < 2 * order) {
      return 2;
    }
    if (identical(knotlist[kindex - (order - 1)], knotlist[order - 1])) {
      return 3;
    }
    for (int i = 0; i < kindex; i++) {
      if (knotlist[i] > knotlist[i + 1])
        return 4;
    }
    int multi = 1;
    for (; kindex >= 1; kindex--) {
      if (knotlist[kindex] - knotlist[kindex - 1] < TOLERANCE) {
        multi++;
        continue;
      }
      if (multi > order) {
        return 5;
      }
      multi = 1;
    }
    if (multi > order) {
      return 5;
    }

    return 0;
  }

  /**
   * Show specified message
   *
   * @param msg
   *            message to be shown
   */
  public void show(final String msg) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO knotvector.show");

  }

  /**
   * Compares two knots for equality
   *
   * @param a
   *            first knot
   * @param b
   *            second knot
   * @return knots are/are not equal
   */
  public static boolean identical(final float a, final float b) {
    return ((a - b) < TOLERANCE) ? true : false;
  }
}
