/*
** License Applicability. Except to the extent portions of this file are
** made subject to an alternative license as permitted in the SGI Free
** Software License B, Version 1.1 (the "License"), the contents of this
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

public abstract class BasicSurfaceEvaluator extends CachingEvaluator {
  public void range2f(long, float[] from, float[] to)              {}
  public void domain2f(float ulo, float uhi, float vlo, float vhi) {}

  public void enable(long type)                                    {}
  public void disable(long type)                                   {}
  public void bgnmap2f(long type)                                  {}
  public void map2f(long type, float ulower, float uupper, long ustride, long uorder, 
                    float vlower, float vupper, long vstride, long vorder, 
                    float[] pts)                                   {}
  public void mapgrid2f(long nu, float u0, float u1,
                        long nv, float v0, float v1)               {}
  public void mapmesh2f(long style, long umin, long umax,
                        long vmin, long vmax)                      {}
  public void evalcoord2f(long type, float u, float v)             {}
  public void evalpoint2i(long u, long v)                          {}
  public void endmap2f()                                           {}

  public void polymode(long style)                                 {}
  public void bgnline()                                            {}
  public void endline()                                            {}
  public void bgnclosedline()                                      {}
  public void endclosedline()                                      {}
  public void bgntmesh()                                           {}
  public void swaptmesh()                                          {}
  public void endtmesh()                                           {}
  public void bgnqstrip()                                          {}
  public void endqstrip()                                          {}

  public void bgntfan()                                            {}
  public void endtfan()                                            {}

  public abstract void evalUStrip(int n_upper, float v_upper, float[] upper_val,
                                  int n_lower, float v_lower, float[] lower_val);
  public abstract void evalVStrip(int n_left, float u_left, float[] left_val,
                                  int n_right, float u_right, float[] right_val);
  public abstract void inDoEvalCoord2NOGE(float u, float v, float[] ret_point, float[] ret_normal);
  public abstract void inDoEvalCoord2NOGE_BU(float u, float v, float[] ret_point, float[] ret_normal);
  public abstract void inDoEvalCoord2NOGE_BV(float u, float v, float[] ret_point, float[] ret_normal);
  public abstract void inPreEvaluateBV_intfac(float v);
  public abstract void inPreEvaluateBU_intfac(float u);
}
