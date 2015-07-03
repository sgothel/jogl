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
 * Class for woking with linked list of curves
 * @author Tomas Hrasky
 *
 */
public class Curvelist {

  /**
   * Head of linked list
   */
  private Curve curve;

  /**
   * Holds conversion range borders
   */
  float[] range;

  /**
   * Subdivision step size
   */
  public float stepsize;

  /**
   * Do curves need subdivision?
   */
  private boolean needsSubdivision;

  /**
   * Makes new instance on top of specified lis of Quilts
   * @param qlist underlaying list of quilts
   * @param pta range start
   * @param ptb range end
   */
  public Curvelist(final Quilt qlist, final float[] pta, final float[] ptb) {
    // DONE
    curve = null;
    range = new float[3];

    for (Quilt q = qlist; q != null; q = q.next) {
      curve = new Curve(q, pta, ptb, curve);
    }
    range[0] = pta[0];
    range[1] = ptb[0];
    range[2] = range[1] - range[0];
  }

  /**
   * Compute step size
   */
  public void getstepsize() {
    // DONE
    stepsize = range[2];
    Curve c;
    for (c = curve; c != null; c = c.next) {
      c.getStepSize();
      c.clamp();
      stepsize = (c.stepsize < stepsize) ? c.stepsize : stepsize;
      if (c.needsSamplingSubdivision())
        break;
    }
    needsSubdivision = (c != null) ? true : false;

  }

  /**
   * Indicates whether curves need subdivision
   * @return curves need subdivision
   */
  public boolean needsSamplingSubdivision() {
    // DONE
    return needsSubdivision;
  }

  /**
   * Checks for culling
   * @return Subdivider.CULL_TRIVIAL_REJECT or Subdivider.CULL_ACCEPT
   */
  public int cullCheck() {
    // DONE
    for (Curve c = curve; c != null; c = c.next)
      if (c.cullCheck() == Subdivider.CULL_TRIVIAL_REJECT)
        return Subdivider.CULL_TRIVIAL_REJECT;
    return Subdivider.CULL_ACCEPT;
  }
}
