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
 * Class holding curve definition
 * @author Tomáš Hráský
 *
 */
public class Curve {

  /**
   * Maximum coordinates per control point
   */
  private static final int MAXCOORDS = 5;

  /**
   * Max curve order
   */
  private static final int MAXORDER = 24;

  /**
   * Next curve in linked list
   */
  public Curve next;

  /**
   * OpenGL maps
   */
  private Mapdesc mapdesc;

  /**
   * Does the curve need sampling
   */
  private boolean needsSampling;

  /**
   * Culling
   */
  private int cullval;

  /**
   * Number of coords
   */
  private int stride;

  /**
   * Curve order
   */
  private int order;

  /**
   * Holds conversion range borders
   */
  private float[] range;

  /**
   * Subdivision stepsize
   */
  public float stepsize;

  /**
   * Minimal subdivision stepsize
   */
  private float minstepsize;

  /**
   * Sampling points
   */
  float[] spts;

  /**
   * Makes new Curve
   * 
   * @param geo
   * @param pta
   * @param ptb
   * @param c
   *            next curve in linked list
   */
  public Curve(Quilt geo, float[] pta, float[] ptb, Curve c) {

    spts = new float[MAXORDER * MAXCOORDS];

    mapdesc = geo.mapdesc;

    next = c;
    needsSampling = mapdesc.isRangeSampling() ? true : false;

    cullval = mapdesc.isCulling() ? Subdivider.CULL_ACCEPT
      : Subdivider.CULL_TRIVIAL_REJECT;
    order = geo.qspec.get(0).order;
    stride = MAXCOORDS;

    // CArrayOfFloats ps = geo.cpts;
    CArrayOfFloats ps = new CArrayOfFloats(geo.cpts.getArray(), 0);
    CArrayOfQuiltspecs qs = geo.qspec;
    ps.raisePointerBy(qs.get().offset);
    ps.raisePointerBy(qs.get().index * qs.get().order * qs.get().stride);

    if (needsSampling) {
      mapdesc.xformSampling(ps, qs.get().order, qs.get().stride, spts,
                            stride);
    }
    if (cullval == Subdivider.CULL_ACCEPT) {
      //                System.out.println("TODO curve.Curve-cullval");
      // mapdesc.xformCulling(ps,qs.get().order,qs.get().stride,cpts,stride);
    }

    range = new float[3];
    range[0] = qs.get().breakpoints[qs.get().index];
    range[1] = qs.get().breakpoints[qs.get().index + 1];
    range[2] = range[1] - range[0];
    // TODO it is necessary to solve problem with "this" pointer here 
    if (range[0] != pta[0]) {
      //                System.out.println("TODO curve.Curve-range0");
      // Curve lower=new Curve(this,pta,0);
      // lower.next=next;
      // this=lower;
    }
    if (range[1] != ptb[0]) {
      //                System.out.println("TODO curve.Curve-range1");
      // Curve lower=new Curve(this,ptb,0);
    }
  }

  /**
   * Checks culling type
   * @return Subdivider.CULL_ACCEPT
   */
  public int cullCheck() {
    if (cullval == Subdivider.CULL_ACCEPT) {
      //                System.out.println("TODO curve.cullval");
      // cullval=mapdesc.cullCheck(cpts,order,stride);
    }
    // TODO compute cullval and return the computed value
    // return cullval;
    return Subdivider.CULL_ACCEPT;
  }

  /**
   * Computes subdivision step size
   */
  public void getStepSize() {
    minstepsize = 0;
    if (mapdesc.isConstantSampling()) {
      setstepsize(mapdesc.maxrate);
    } else if (mapdesc.isDomainSampling()) {
      setstepsize(mapdesc.maxrate * range[2]);
    } else {
      assert (order <= MAXORDER);

      float tmp[][] = new float[MAXORDER][MAXCOORDS];

      int tstride = (MAXORDER);

      int val = 0;
      // mapdesc.project(spts,stride,tmp,tstride,order);

      //                System.out.println("TODO curve.getsptepsize mapdesc.project");

      if (val == 0) {
        setstepsize(mapdesc.maxrate);
      } else {
        float t = mapdesc.getProperty(NurbsConsts.N_PIXEL_TOLERANCE);
        if (mapdesc.isParametricDistanceSampling()) {
          //                        System.out.println("TODO curve.getstepsize - parametric");
        } else if (mapdesc.isPathLengthSampling()) {
          //                        System.out.println("TODO curve.getstepsize - pathlength");
        } else {
          setstepsize(mapdesc.maxrate);
        }
      }

    }

  }

  /**
   * Sets maximum subdivision step size
   * @param max maximum subdivision step size
   */
  private void setstepsize(float max) {
    // DONE
    stepsize = (max >= 1) ? (range[2] / max) : range[2];
    minstepsize = stepsize;
  }

  /**
   * Clamps the curve
   */
  public void clamp() {
    // DONE
    if (stepsize < minstepsize)
      stepsize = mapdesc.clampfactor * minstepsize;
  }

  /**
   * Tells whether curve needs subdivision
   * 
   * @return curve needs subdivison
   */
  public boolean needsSamplingSubdivision() {
    return (stepsize < minstepsize);
  }
}
