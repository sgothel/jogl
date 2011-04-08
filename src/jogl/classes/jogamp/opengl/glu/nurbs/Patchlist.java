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
 * List of patches
 * @author Tomáš Hráský
 *
 */
public class Patchlist {

  /**
   * Array of ranges
   */
  public Pspec[] pspec;

  /**
   * head of list of patches
   */
  private Patch patch;

  /**
   * Makes new list of patches 
   * @param quilts list of quilts
   * @param pta low border
   * @param ptb high border
   */
  public Patchlist(Quilt quilts, float[] pta, float[] ptb) {
    // DONE
    patch = null;

    for (Quilt q = quilts; q != null; q = q.next)
      patch = new Patch(q, pta, ptb, patch);
    pspec[0] = new Pspec();
    pspec[0].range[0] = pta[0];
    pspec[0].range[1] = ptb[0];
    pspec[0].range[2] = ptb[0] - pta[0];
    pspec[1] = new Pspec();
    pspec[1].range[0] = pta[1];
    pspec[1].range[1] = ptb[1];
    pspec[1].range[2] = ptb[1] - pta[1];

  }

  /**
   * Empty constructor
   * @param patchlist
   * @param param
   * @param mid
   */
  public Patchlist(Patchlist patchlist, int param, float mid) {
    // TODO Auto-generated constructor stub
    //            System.out.println("TODO patchlist.konstruktor 2");
  }

  /**
   * Empty method
   * @return 0
   */
  public int cullCheck() {
    // TODO Auto-generated method stub
    //            System.out.println("TODO patchlist.cullcheck");
    return 0;
  }

  /**
   * Empty method
   */
  public void getstepsize() {
    //            System.out.println("TODO patchlist.getsptepsize");
    // TODO Auto-generated method stub

  }

  /**
   * Empty method
   * @return false
   */
  public boolean needsSamplingSubdivision() {
    // TODO Auto-generated method stub
    //            System.out.println("patchlist.needsSamplingSubdivision");
    return false;
  }

  /**
   * Empty method
   * @param i
   * @return false
   */
  public boolean needsSubdivision(int i) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO patchlist.needsSubdivision");
    return false;
  }

  /**
   * Empty method
   * @return false
   */
  public boolean needsNonSamplingSubdivision() {
    // TODO Auto-generated method stub
    //            System.out.println("TODO patchlist.needsNonSamplingSubdivision");
    return false;
  }

  /**
   * Empty method
   */
  public void bbox() {
    // TODO Auto-generated method stub
    //            System.out.println("TODO patchlist.bbox");
  }
}
