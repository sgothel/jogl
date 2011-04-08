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
 * Class holding properties of OpenGL map
 * @author Tomas Hrasky
 *
 */
public class Mapdesc {

  /**
   * Maximum control point coords
   */
  private static final int MAXCOORDS = 5;

  /**
   * Next description in list
   */
  public Mapdesc next;

  /**
   * Is map rational
   */
  public int isrational;

  /**
   * Number of control point coords
   */
  public int ncoords;

  /**
   * Map type
   */
  private int type;

  /**
   * Number of homogenous coords
   */
  private int hcoords;

  /**
   * Number of inhomogenous coords
   */
  private int inhcoords;

  /**
   * Not used
   */
  private int mask;

  /**
   * Value of N_PIXEL_TOLERANCE property
   */
  private float pixel_tolerance;

  /**
   * Value of N_ERROR_TOLERANCE property
   */
  private float error_tolerance;

  /**
   * Value of N_BBOX_SUBDIVIDING property
   */
  private float bbox_subdividing;

  /**
   * Value of N_CULLING property
   */
  private float culling_method;

  /**
   * Value of N_SAMPLINGMETHOD property
   */
  private float sampling_method;

  /**
   * Value of N_CLAMPFACTOR property
   */
  float clampfactor;

  /**
   * Value of N_MINSAVINGS property 
   */
  private float minsavings;

  /**
   * Steps in u direction
   */
  private float s_steps;

  /**
   * Steps in v direction
   */
  private float t_steps;

  /**
   * Maximal step
   */
  float maxrate;

  /**
   * Maximal u direction step
   */
  private float maxsrate;

  /**
   * Maximal v direction step
   */
  private float maxtrate;

  /**
   * Not used
   */
  private float[][] bmat;

  /**
   * Sampling matrix
   */
  private float[][] smat;

  /**
   * Not used
   */
  private float[][] cmat;

  /**
   * Not used
   */
  private float[] bboxsize;

  /**
   * Makes new mapdesc 
   * @param type map type
   * @param rational is rational
   * @param ncoords number of control points coords
   * @param backend backend object
   */
  public Mapdesc(int type, int rational, int ncoords, Backend backend) {
    // DONE
    this.type = type;
    this.isrational = rational;
    this.ncoords = ncoords;
    this.hcoords = ncoords + (isrational > 0 ? 0 : 1);
    this.inhcoords = ncoords - (isrational > 0 ? 1 : 0);
    this.mask = ((1 << (inhcoords * 2)) - 1);
    next = null;

    assert (hcoords <= MAXCOORDS);
    assert (inhcoords >= 1);

    pixel_tolerance = 1f;
    error_tolerance = 1f;
    bbox_subdividing = NurbsConsts.N_NOBBOXSUBDIVISION;
    culling_method = NurbsConsts.N_NOCULLING;
    sampling_method = NurbsConsts.N_NOSAMPLING;
    clampfactor = NurbsConsts.N_NOCLAMPING;
    minsavings = NurbsConsts.N_NOSAVINGSSUBDIVISION;
    s_steps = 0f;
    t_steps = 0f;

    maxrate = (s_steps < 0) ? 0 : s_steps;
    maxsrate = (s_steps < 0) ? 0 : s_steps;
    maxtrate = (t_steps < 0) ? 0 : t_steps;
    bmat = new float[MAXCOORDS][MAXCOORDS];
    cmat = new float[MAXCOORDS][MAXCOORDS];
    smat = new float[MAXCOORDS][MAXCOORDS];

    identify(bmat);
    identify(cmat);
    identify(smat);
    bboxsize = new float[MAXCOORDS];
    for (int i = 0; i < inhcoords; i++)
      bboxsize[i] = 1;
  }

  /**
   * Make matrix identity matrix
   * @param arr matrix
   */
  private void identify(float[][] arr) {
    // DONE
    for (int i = 0; i < MAXCOORDS; i++)
      for (int j = 0; j < MAXCOORDS; j++)
        arr[i][j] = 0;
    for (int i = 0; i < MAXCOORDS; i++)
      arr[i][i] = 1;

  }

  /**
   * Tells whether tag is property tag
   * @param tag property tag
   * @return is/is not property
   */
  public boolean isProperty(int tag) {
    boolean ret;
    switch (tag) {
    case NurbsConsts.N_PIXEL_TOLERANCE:
    case NurbsConsts.N_ERROR_TOLERANCE:
    case NurbsConsts.N_CULLING:
    case NurbsConsts.N_BBOX_SUBDIVIDING:
    case NurbsConsts.N_S_STEPS:
    case NurbsConsts.N_T_STEPS:
    case NurbsConsts.N_SAMPLINGMETHOD:
    case NurbsConsts.N_CLAMPFACTOR:
    case NurbsConsts.N_MINSAVINGS:
      ret = true;
      break;
    default:
      ret = false;
      break;
    }
    return ret;
  }

  /**
   * Returns number of control points' coords
   * @return number of control points' coords
   */
  public int getNCoords() {
    return ncoords;
  }

  /**
   * Returns map type
   * @return map type
   */
  public int getType() {
    return type;
  }

  /**
   * Tells whether map is range sampling
   * @return is map range sampling
   */
  public boolean isRangeSampling() {
    // DONE
    return (isParametricDistanceSampling() || isPathLengthSampling()
            || isSurfaceAreaSampling() || isObjectSpaceParaSampling() || isObjectSpacePathSampling());
  }

  /**
   * Tells whether map is object space sampling
   * @return is map object space sampling
   */
  private boolean isObjectSpacePathSampling() {
    // DONE
    return sampling_method == NurbsConsts.N_OBJECTSPACE_PATH;
  }

  /**
   * Tells whether map is object space parasampling
   * @return is map object space parasampling
   */
  private boolean isObjectSpaceParaSampling() {
    // DONE
    return sampling_method == NurbsConsts.N_OBJECTSPACE_PARA;
  }

  /**
   * Tells whether map is area sampling surface
   * @return is map area sampling surface
   */
  private boolean isSurfaceAreaSampling() {
    // DONE
    return sampling_method == NurbsConsts.N_SURFACEAREA;
  }

  /**
   * Tells whether map is path length sampling
   * @return is map path length sampling
   */
  boolean isPathLengthSampling() {
    // DONE
    return sampling_method == NurbsConsts.N_PATHLENGTH;
  }

  /**
   * Tells whether map is parametric distance sampling
   * @return is map parametric distance sampling
   */
  boolean isParametricDistanceSampling() {
    // DONE
    return sampling_method == NurbsConsts.N_PARAMETRICDISTANCE;
  }

  /**
   * Tells whether map is culling 
   * @return is map culling
   */
  public boolean isCulling() {
    // DONE
    return culling_method != NurbsConsts.N_NOCULLING ? true : false;
  }

  /**
   * Tells whether map is constantly sampling 
   * @return is map constant sampling
   */
  public boolean isConstantSampling() {
    return (sampling_method == NurbsConsts.N_FIXEDRATE) ? true : false;
  }

  /**
   * Tells whether map is domain sampling 
   * @return is map domain sampling
   */
  public boolean isDomainSampling() {
    return (sampling_method == NurbsConsts.N_DOMAINDISTANCE) ? true : false;
  }

  /**
   * Returns property of specified tag value 
   * @param tag property tag
   * @return property value
   */
  public float getProperty(int tag) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO mapdesc.getproperty");
    return 0;
  }

  /**
   * Sets property with given tag
   * @param tag property tag
   * @param value desired value
   */
  public void setProperty(int tag, float value) {
    // TODO Auto-generated method stub
    switch (tag) {
    case NurbsConsts.N_PIXEL_TOLERANCE:
      pixel_tolerance = value;
      break;
    case NurbsConsts.N_ERROR_TOLERANCE:
      error_tolerance = value;
      break;
    case NurbsConsts.N_CULLING:
      culling_method = value;
      break;
    case NurbsConsts.N_BBOX_SUBDIVIDING:
      if (value <= 0)
        value = NurbsConsts.N_NOBBOXSUBDIVISION;
      bbox_subdividing = value;
      break;
    case NurbsConsts.N_S_STEPS:
      if (value < 0)
        value = 0;
      s_steps = value;
      maxrate = value;
      maxsrate = value;
      break;
    case NurbsConsts.N_T_STEPS:
      if (value < 0)
        value = 0;
      t_steps = value;
      maxtrate = value;
      break;
    case NurbsConsts.N_SAMPLINGMETHOD:
      sampling_method = value;
      break;
    case NurbsConsts.N_CLAMPFACTOR:
      if (value < 0)
        value = 0;
      clampfactor = value;
      break;
    case NurbsConsts.N_MINSAVINGS:
      if (value <= 0)
        value = NurbsConsts.N_NOSAVINGSSUBDIVISION;
      minsavings = value;
      break;
    }
  }

  /**
   * Samples curve
   * @param pts control points
   * @param order curve order
   * @param stride number of control points' coordinates
   * @param sp breakpoints
   * @param outstride output number of control points' coordinates
   */
  public void xformSampling(CArrayOfFloats pts, int order, int stride,
                            float[] sp, int outstride) {
    // DONE
    xFormMat(smat, pts, order, stride, sp, outstride);
  }

  /**
   * Empty method
   * @param mat sampling matrix
   * @param pts ontrol points
   * @param order curve order
   * @param stride number of control points' coordinates
   * @param cp breakpoints
   * @param outstride output number of control points' coordinates
   */
  private void xFormMat(float[][] mat, CArrayOfFloats pts, int order,
            int stride, float[] cp, int outstride) {
    // TODO Auto-generated method stub

    //        System.out.println("TODO mapdsc.xformmat ; change cp from float[] to carrayoffloats");

    if (isrational > 0) {

    } else {

    }
  }
}
