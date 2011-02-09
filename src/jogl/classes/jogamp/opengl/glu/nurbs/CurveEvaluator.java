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
 * Class rendering curves with OpenGL
 * @author Tomáš Hráský
 *
 */
public interface CurveEvaluator {
  /**
   * Pushes eval bit
   */
  public void bgnmap1f();

  /**
   * Pops all OpenGL attributes
   */
  public void endmap1f() ;

  /**
   * Initializes opengl evaluator
   * @param type curve type
   * @param ulo lowest u
   * @param uhi highest u
   * @param stride control point coords
   * @param order curve order
   * @param ps control points
   */
  public void map1f(int type, float ulo, float uhi, int stride, int order,
                    CArrayOfFloats ps) ;

  /**
   * Calls opengl enable
   * @param type what to enable
   */
  public void enable(int type) ;

  /**
   * Calls glMapGrid1f
   * @param nu steps
   * @param u1 low u
   * @param u2 high u
   */
  public void mapgrid1f(int nu, float u1, float u2) ;

  /**
   * Evaluates a curve using glEvalMesh1f
   * @param style Backend.N_MESHFILL/N_MESHLINE/N_MESHPOINT
   * @param from lowest param
   * @param to highest param
   */
  public void mapmesh1f(int style, int from, int to) ;
}
