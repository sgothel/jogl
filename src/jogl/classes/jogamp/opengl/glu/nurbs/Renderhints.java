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
 * Class holding rendering params
 * @author Tomas Hrasky
 *
 */
public class Renderhints {

  /**
   * Check for errors
   */
  public int errorchecking;

  /**
   * Maximum subdivisions
   */
  public int maxsubdivisions;

  /**
   * Number of subdivisions
   */
  private int subdivisions;

  /**
   * Display method
   */
  int display_method;

  /**
   * Output triangles
   */
  int wiretris;

  /**
   * Output quads
   */
  int wirequads;

  /**
   * Makes new Renderinghints
   */
  public Renderhints() {
    display_method = NurbsConsts.N_FILL;
    errorchecking = NurbsConsts.N_MSG;
    subdivisions = 6;
    // tmp1=0;
  }

  /**
   * Set property value
   * @param prop property
   */
  public void setProperty(final Property prop) {
    switch (prop.type) {
    case NurbsConsts.N_DISPLAY:
      display_method = (int) prop.value;
      break;
    case NurbsConsts.N_ERRORCHECKING:
      errorchecking = (int) prop.value;
      break;
    case NurbsConsts.N_SUBDIVISIONS:
      subdivisions = (int) prop.value;
      break;
    default:
      // abort - end program
      break;
    }
  }

  /**
   * Initialization
   */
  public void init() {
    // DONE
    maxsubdivisions = subdivisions;
    if (maxsubdivisions < 0)
      maxsubdivisions = 0;

    if (display_method == NurbsConsts.N_FILL) {
      wiretris = 0;
      wirequads = 0;
    } else if (display_method == NurbsConsts.N_OUTLINE_TRI) {
      wiretris = 1;
      wirequads = 0;
    } else if (display_method == NurbsConsts.N_OUTLINE_QUAD) {
      wiretris = 0;
      wirequads = 1;
    } else {
      wiretris = 1;
      wirequads = 1;
    }
  }
}
