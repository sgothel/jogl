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
 * Class rendering curves with OpenGL
 * @author Tomáš Hráský
 *
 */
class GL2CurveEvaluator implements CurveEvaluator {

  /**
   * Output triangles (for callback) or render curve 
   */
  private boolean output_triangles;

  /**
   * OpenGL object
   */
  private GL2 gl;

  /**
   * Not used
   */
  private int vertex_flag;

  /**
   * Not used
   */
  private int normal_flag;

  /**
   * Not used
   */
  private int color_flag;

  /**
   * Not used
   */
  private int texcoord_flag;

  /**
   * Number of bezier arc - used for color distinguishing of arcs forming NURBS curve
   */
  private int poradi;

  /**
   * Makes new Evaluator
   */
  public GL2CurveEvaluator() {
    gl = GLUgl2.getCurrentGL2();
  }

  /**
   * Pushes eval bit
   */
  public void bgnmap1f() {
    // DONE
    if (output_triangles) {
      vertex_flag = 0;
      normal_flag = 0;
      color_flag = 0;
      texcoord_flag = 0;
    } else {
      gl.glPushAttrib(GL2.GL_EVAL_BIT);
    }

  }

  /**
   * Pops all OpenGL attributes
   */
  public void endmap1f() {
    // DONE
    if (output_triangles) {

    } else {
      gl.glPopAttrib();
    }

  }

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
                    CArrayOfFloats ps) {
    if (output_triangles) {
      // TODO code for callback (output_triangles probably indicates callback)
      //                System.out.println("TODO curveevaluator.map1f-output_triangles");
    } else {
      gl.glMap1f(type, ulo, uhi, stride, order, ps.getArray(), ps
                 .getPointer());

      // DEBUG - drawing bézier control points
      // gl.glColor3d(.5,.5,.5);
      // gl.glPointSize(5);
      // gl.glBegin(GL2.GL_POINTS);
      // float[] ctrlpoints=ps.getArray();
      // for(int i=ps.getPointer();i<ps.getPointer()+order;i++){
      // gl.glVertex3d(ctrlpoints[i * 4], ctrlpoints[i * 4 + 1],0);
      // }
      // gl.glEnd();
    }

  }

  /**
   * Calls opengl enable
   * @param type what to enable
   */
  public void enable(int type) {
    // DONE
    gl.glEnable(type);
  }

  /**
   * Calls glMapGrid1f
   * @param nu steps
   * @param u1 low u
   * @param u2 high u
   */
  public void mapgrid1f(int nu, float u1, float u2) {
    if (output_triangles) {
      //                System.out.println("TODO curveevaluator.mapgrid1f");
    } else
      gl.glMapGrid1f(nu, u1, u2);
    //            // System.out.println("upravit NU");
    // gl.glMapGrid1f(50,u1,u2);
  }

  /**
   * Evaluates a curve using glEvalMesh1f
   * @param style Backend.N_MESHFILL/N_MESHLINE/N_MESHPOINT
   * @param from lowest param
   * @param to highest param
   */
  public void mapmesh1f(int style, int from, int to) {
    /* //DEBUG drawing control points
       this.poradi++;
       if (poradi % 2 == 0)
       gl.glColor3f(1, 0, 0);
       else
       gl.glColor3f(0, 1, 0);
    */
    if (output_triangles) {
      // TODO code for callback
      //            System.out.println("TODO openglcurveevaluator.mapmesh1f output_triangles");
    } else {
      switch (style) {
      case Backend.N_MESHFILL:
      case Backend.N_MESHLINE:
        gl.glEvalMesh1(GL2.GL_LINE, from, to);
        break;
      case Backend.N_MESHPOINT:
        gl.glEvalMesh1(GL2.GL_POINT, from, to);
        break;
      }
    }
  }
}
