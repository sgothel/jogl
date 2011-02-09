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
 * Class rendering surfaces with OpenGL
 * @author Tomas Hrasky
 *
 */
public interface SurfaceEvaluator {

  /**
   * Pushes eval bit
   */
  public void bgnmap2f() ;

  /**
   * Sets  glPolygonMode
   * @param style polygon mode (N_MESHFILL/N_MESHLINE/N_MESHPOINT)
   */
  public void polymode(int style) ;

  /**
   * Pops all attributes
   */
  public void endmap2f() ;

  /**
   * Empty method
   * @param ulo
   * @param uhi
   * @param vlo
   * @param vhi
   */
  public void domain2f(float ulo, float uhi, float vlo, float vhi) ;

  /**
   * Defines 2D mesh
   * @param nu number of steps in u direction
   * @param u0 lowest u
   * @param u1 highest u
   * @param nv number of steps in v direction
   * @param v0 lowest v
   * @param v1 highest v
   */
  public void mapgrid2f(int nu, float u0, float u1, int nv, float v0, float v1) ;

  /**
   * Evaluates surface
   * @param style surface style
   * @param umin minimum U
   * @param umax maximum U
   * @param vmin minimum V
   * @param vmax maximum V
   */
  public void mapmesh2f(int style, int umin, int umax, int vmin, int vmax) ;

  /**
   * Initializes evaluator
   * @param type surface type
   * @param ulo lowest u
   * @param uhi highest u
   * @param ustride number of objects between control points in u direction
   * @param uorder surface order in u direction
   * @param vlo lowest v
   * @param vhi highest v
   * @param vstride number of control points' coords
   * @param vorder surface order in v direction
   * @param pts control points
   */
  public void map2f(int type, float ulo, float uhi, int ustride, int uorder,
                    float vlo, float vhi, int vstride, int vorder, CArrayOfFloats pts) ;

  /**
   * Calls opengl enable
   * @param type what to enable
   */
  public void enable(int type) ;
}
