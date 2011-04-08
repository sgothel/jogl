package jogamp.opengl.glu.gl2.nurbs;
import jogamp.opengl.glu.nurbs.*;

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

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.gl2.GLUgl2;

/**
 * Class rendering surfaces with OpenGL
 * @author Tomas Hrasky
 *
 */
class GL2SurfaceEvaluator implements SurfaceEvaluator {

  /**
   * JOGL OpenGL object
   */
  private GL2 gl;

  /**
   * Output triangles (callback)
   */
  private boolean output_triangles;

  /**
   * Number of patch - used for distinguishing bezier plates forming NURBS surface with different colors
   */
  private int poradi;

  /**
   * Creates new evaluator
   */
  public GL2SurfaceEvaluator() {
    gl = GLUgl2.getCurrentGL2();
  }

  /**
   * Pushes eval bit
   */
  public void bgnmap2f() {

    if (output_triangles) {
      // TODO outp triangles surfaceevaluator bgnmap2f
      //            System.out.println("TODO surfaceevaluator.bgnmap2f output triangles");
    } else {
      gl.glPushAttrib(GL2.GL_EVAL_BIT);
      //                System.out.println("TODO surfaceevaluator.bgnmap2f glgetintegerv");
    }

  }

  /**
   * Sets  glPolygonMode
   * @param style polygon mode (N_MESHFILL/N_MESHLINE/N_MESHPOINT)
   */
  public void polymode(int style) {
    if (!output_triangles) {
      switch (style) {
      default:
      case NurbsConsts.N_MESHFILL:
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
        break;
      case NurbsConsts.N_MESHLINE:
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
        break;
      case NurbsConsts.N_MESHPOINT:
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_POINT);
        break;
      }
    }

  }

  /**
   * Pops all attributes
   */
  public void endmap2f() {
    // TODO Auto-generated method stub
    if (output_triangles) {
      //            System.out.println("TODO surfaceevaluator.endmap2f output triangles");
    } else {
      gl.glPopAttrib();
      // TODO use LOD
    }
  }

  /**
   * Empty method
   * @param ulo
   * @param uhi
   * @param vlo
   * @param vhi
   */
  public void domain2f(float ulo, float uhi, float vlo, float vhi) {
    // DONE
  }

  /**
   * Defines 2D mesh
   * @param nu number of steps in u direction
   * @param u0 lowest u
   * @param u1 highest u
   * @param nv number of steps in v direction
   * @param v0 lowest v
   * @param v1 highest v
   */
  public void mapgrid2f(int nu, float u0, float u1, int nv, float v0, float v1) {

    if (output_triangles) {
      //            System.out.println("TODO openglsurfaceavaluator.mapgrid2f output_triangles");
    } else {
      gl.glMapGrid2d(nu, u0, u1, nv, v0, v1);
    }

  }

  /**
   * Evaluates surface
   * @param style surface style
   * @param umin minimum U
   * @param umax maximum U
   * @param vmin minimum V
   * @param vmax maximum V
   */
  public void mapmesh2f(int style, int umin, int umax, int vmin, int vmax) {
    if (output_triangles) {
      //            System.out.println("TODO openglsurfaceavaluator.mapmesh2f output_triangles");
    } else {
      /* //DEBUG - draw control points
         this.poradi++;
         if (poradi % 2 == 0)
         gl.glColor3f(1, 0, 0);
         else if (poradi % 2 == 1)
         gl.glColor3f(0, 1, 0);
      */
      switch (style) {
      case NurbsConsts.N_MESHFILL:
        gl.glEvalMesh2(GL2.GL_FILL, umin, umax, vmin, vmax);
        break;
      case NurbsConsts.N_MESHLINE:
        gl.glEvalMesh2(GL2.GL_LINE, umin, umax, vmin, vmax);
        break;
      case NurbsConsts.N_MESHPOINT:
        gl.glEvalMesh2(GL2.GL_POINT, umin, umax, vmin, vmax);
        break;
      }
    }
  }

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
                    float vlo, float vhi, int vstride, int vorder, CArrayOfFloats pts) {
    // TODO Auto-generated method stub
    if (output_triangles) {
      //            System.out.println("TODO openglsurfaceevaluator.map2f output_triangles");
    } else {
      gl.glMap2f(type, ulo, uhi, ustride, uorder, vlo, vhi, vstride,
                 vorder, pts.getArray(), pts.getPointer());
    }
  }

  /**
   * Calls opengl enable
   * @param type what to enable
   */
  public void enable(int type) {
    //DONE
    gl.glEnable(type);
  }
}
