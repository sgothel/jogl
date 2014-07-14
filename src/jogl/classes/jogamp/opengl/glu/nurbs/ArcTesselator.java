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
 * Class for arc tesselation
 * @author Tomas Hrasky
 *
 */
public class ArcTesselator {

  /**
   * Makes given arc an bezier arc
   * @param arc arc to work with
   * @param s1 minimum s param
   * @param s2 maximum s param
   * @param t1 minimum t param
   * @param t2 maximum s param
   */
  public void bezier(final Arc arc, final float s1, final float s2, final float t1, final float t2) {
    // DONE
    final TrimVertex[] p = new TrimVertex[2];
    p[0] = new TrimVertex();
    p[1] = new TrimVertex();
    arc.pwlArc = new PwlArc(2, p);
    p[0].param[0] = s1;
    p[0].param[1] = s2;
    p[1].param[0] = t1;
    p[1].param[1] = t2;
    arc.setbezier();
  }

  /**
   * Empty method
   * @param newright arc to work with
   * @param s first tail
   * @param t2 second tail
   * @param t1 third tail
   * @param f stepsize
   */
  public void pwl_right(final Arc newright, final float s, final float t1, final float t2, final float f) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO arctesselator.pwl_right");
  }

  /**
   * Empty method
   * @param newright arc to work with
   * @param s first tail
   * @param t2 second tail
   * @param t1 third tail
   * @param f stepsize
   */
  public void pwl_left(final Arc newright, final float s, final float t2, final float t1, final float f) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO arctesselator.pwl_left");
  }
}
