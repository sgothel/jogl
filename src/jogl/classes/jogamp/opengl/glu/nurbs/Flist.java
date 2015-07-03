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

import java.util.Arrays;

/**
 * List of breakpoints
 * @author Tomas Hrasky
 *
 */
public class Flist {

  /**
   * Data elements end index
   *
   */
  public int end;

  /**
   *Data elements start index
   */
  public int start;

  /**
   * Breakpoint values
   */
  public float[] pts;

  /**
   * Number of array fields
   */
  private int npts;

  /**
   * Grows list
   * @param maxpts maximum desired size
   */
  public void grow(final int maxpts) {
    // DONE
    if (npts < maxpts) {
      // npts=2*maxpts;
      npts = maxpts;
      pts = new float[npts];
    }
    start = 0;
    end = 0;
  }

  /**
   * Removes duplicate array elemnts
   */
  public void filter() {
    // INFO the aim of this method is to remove duplicates from array

    Arrays.sort(pts);

    start = 0;

    int j = 0;

    for (int i = 1; i < end; i++) {
      if (pts[i] == pts[i - j - 1])
        j++;
      pts[i - j] = pts[i];
    }

    end -= j;

  }

  /**
   * Sets start and and to real start and end of array elements
   * @param from start from
   * @param to end at
   */
  public void taper(final float from, final float to) {
    // DONE

    while (pts[start] != from) {
      start++;
    }

    while (pts[end - 1] != to) {
      end--;
    }

  }

  /**
   * Adds breakpoint value
   * @param f value
   */
  public void add(final float f) {
    //DONE
    pts[end++] = f;
  }
}
