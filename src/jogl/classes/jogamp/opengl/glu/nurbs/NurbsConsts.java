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
 * Class hodling NURBS constants as seen in OpenGL GLU documentation
 * @author JOGL project
 *
 */
public class NurbsConsts {
  /*
   * NURBS Properties - one set per map, each takes a single INREAL arg
   */
  public static final int N_SAMPLING_TOLERANCE = 1;

  public static final int N_S_RATE = 6;

  public static final int N_T_RATE = 7;

  public static final int N_CLAMPFACTOR = 13;

  public static final float N_NOCLAMPING = 0.0f;

  public static final int N_MINSAVINGS = 14;

  public static final float N_NOSAVINGSSUBDIVISION = 0.0f;

  /*
   * NURBS Properties - one set per map, each takes an enumerated value
   */
  public static final int N_CULLING = 2;

  public static final float N_NOCULLING = 0.0f;

  public static final float N_CULLINGON = 1.0f;

  public static final int N_SAMPLINGMETHOD = 10;

  public static final float N_NOSAMPLING = 0.0f;

  public static final float N_FIXEDRATE = 3.0f;

  public static final float N_DOMAINDISTANCE = 2.0f;

  public static final float N_PARAMETRICDISTANCE = 5.0f;

  public static final float N_PATHLENGTH = 6.0f;

  public static final float N_SURFACEAREA = 7.0f;

  public static final float N_OBJECTSPACE_PARA = 8.0f;

  public static final float N_OBJECTSPACE_PATH = 9.0f;

  public static final int N_BBOX_SUBDIVIDING = 17;

  public static final float N_NOBBOXSUBDIVISION = 0.0f;

  public static final float N_BBOXTIGHT = 1.0f;

  public static final float N_BBOXROUND = 2.0f;

  /*
   * NURBS Rendering Properties - one set per renderer each takes an
   * enumerated value
   */
  public static final int N_DISPLAY = 3;

  public static final int N_FILL = 1;

  public static final int N_OUTLINE_POLY = 2;

  public static final int N_OUTLINE_TRI = 3;

  public static final int N_OUTLINE_QUAD = 4;

  public static final int N_OUTLINE_PATCH = 5;

  public static final int N_OUTLINE_PARAM = 6;

  public static final int N_OUTLINE_PARAM_S = 7;

  public static final int N_OUTLINE_PARAM_ST = 8;

  public static final int N_OUTLINE_SUBDIV = 9;

  public static final int N_OUTLINE_SUBDIV_S = 10;

  public static final int N_OUTLINE_SUBDIV_ST = 11;

  public static final int N_ISOLINE_S = 12;

  public static final int N_ERRORCHECKING = 4;

  public static final int N_NOMSG = 0;

  public static final int N_MSG = 1;

  /* GL 4.0 propeties not defined above */

  public static final int N_PIXEL_TOLERANCE = N_SAMPLING_TOLERANCE;

  public static final int N_ERROR_TOLERANCE = 20;

  public static final int N_SUBDIVISIONS = 5;

  public static final int N_TILES = 8;

  public static final int N_TMP1 = 9;

  public static final int N_TMP2 = N_SAMPLINGMETHOD;

  public static final int N_TMP3 = 11;

  public static final int N_TMP4 = 12;

  public static final int N_TMP5 = N_CLAMPFACTOR;

  public static final int N_TMP6 = N_MINSAVINGS;

  public static final int N_S_STEPS = N_S_RATE;

  public static final int N_T_STEPS = N_T_RATE;

  /*
   * NURBS Rendering Properties - one set per map, each takes an INREAL matrix
   * argument
   */
  public static final int N_CULLINGMATRIX = 1;

  public static final int N_SAMPLINGMATRIX = 2;

  public static final int N_BBOXMATRIX = 3;

  /*
   * NURBS Rendering Properties - one set per map, each takes an INREAL vector
   * argument
   */
  public static final int N_BBOXSIZE = 4;

  /* type argument for trimming curves */

  public static final int N_P2D = 0x8;

  public static final int N_P2DR = 0xd;

  public static final int N_MESHLINE = 1;

  public static final int N_MESHFILL = 0;

  public static final int N_MESHPOINT = 2;
}
