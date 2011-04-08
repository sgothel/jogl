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
 * Class responsible for rendering
 * @author Tomas Hrasky
 *
 */
public abstract class Backend {

  /**
   * Fill surface
   */
  public static final int N_MESHFILL = 0;

  /**
   * Draw surface as wire model
   */
  public static final int N_MESHLINE = 1;

  /**
   * Draw surface with points
   */
  public static final int N_MESHPOINT = 2;

  /**
   * Object rendering curves
   */
  protected CurveEvaluator curveEvaluator;

  /**
   * Object rendering surfaces
   */
  protected SurfaceEvaluator surfaceEvaluator;

  /**
   * Makes new backend 
   */
  public Backend() {
    // curveEvaluator = new OpenGLCurveEvaluator();
    // surfaceEvaluator = new OpenGLSurfaceEvaluator();
  }

  /**
   * Begin a curve
   */
  public void bgncurv() {
    // DONE
    curveEvaluator.bgnmap1f();

  }

  /**
   * End a curve
   */
  public void endcurv() {
    // DONE
    curveEvaluator.endmap1f();

  }

  /**
   * Make cuve with given parameters
   * @param type curve type
   * @param ps control points
   * @param stride control points coordinates number
   * @param order order of curve
   * @param ulo smallest u
   * @param uhi highest u
   */
  public void curvpts(int type, CArrayOfFloats ps, int stride, int order,
                      float ulo, float uhi) {
    // DONE
    curveEvaluator.map1f(type, ulo, uhi, stride, order, ps);
    curveEvaluator.enable(type);
  }

  /**
   * Draw curve
   * @param u1 smallest u
   * @param u2 highest u
   * @param nu number of pieces
   */
  public void curvgrid(float u1, float u2, int nu) {
    // DONE
    curveEvaluator.mapgrid1f(nu, u1, u2);

  }

  /**
   * Evaluates curve mesh
   * @param from low param
   * @param n step
   */
  public void curvmesh(int from, int n) {
    // DONE
    curveEvaluator.mapmesh1f(N_MESHFILL, from, from + n);
  }

  /**
   * Begin surface
   * @param wiretris use triangles
   * @param wirequads use quads
   */
  public void bgnsurf(int wiretris, int wirequads) {
    // DONE
    surfaceEvaluator.bgnmap2f();

    if (wiretris > 0)
      surfaceEvaluator.polymode(NurbsConsts.N_MESHLINE);
    else
      surfaceEvaluator.polymode(NurbsConsts.N_MESHFILL);
  }

  /**
   * End surface
   */
  public void endsurf() {
    // DONE
    surfaceEvaluator.endmap2f();
  }

  /**
   * Empty method
   * @param ulo low u param
   * @param uhi hig u param
   * @param vlo low v param
   * @param vhi high v param
   */
  public void patch(float ulo, float uhi, float vlo, float vhi) {
    // DONE
    surfaceEvaluator.domain2f(ulo, uhi, vlo, vhi);
  }

  /**
   * Draw surface
   * @param u0 lowest u
   * @param u1 highest u
   * @param nu number of pieces in u direction
   * @param v0 lowest v
   * @param v1 highest v
   * @param nv number of pieces in v direction
   */
  public void surfgrid(float u0, float u1, int nu, float v0, float v1, int nv) {
    // DONE
    surfaceEvaluator.mapgrid2f(nu, u0, u1, nv, v0, v1);

  }

  /**
   * Evaluates surface mesh
   * @param u u param
   * @param v v param
   * @param n step in u direction
   * @param m step in v direction
   */
  public void surfmesh(int u, int v, int n, int m) {
    //            System.out.println("TODO backend.surfmesh wireframequads");
    // TODO wireframequads
    surfaceEvaluator.mapmesh2f(NurbsConsts.N_MESHFILL, u, u + n, v, v + m);
  }

  /**
   * Make surface
   * @param type surface type
   * @param pts control points
   * @param ustride control points coordinates in u direction
   * @param vstride control points coordinates in v direction
   * @param uorder surface order in u direction
   * @param vorder surface order in v direction
   * @param ulo lowest u
   * @param uhi hightest u
   * @param vlo lowest v
   * @param vhi hightest v
   */
  public void surfpts(int type, CArrayOfFloats pts, int ustride, int vstride,
                      int uorder, int vorder, float ulo, float uhi, float vlo, float vhi) {
    // DONE
    surfaceEvaluator.map2f(type, ulo, uhi, ustride, uorder, vlo, vhi,
                           vstride, vorder, pts);
    surfaceEvaluator.enable(type);

  }
}
