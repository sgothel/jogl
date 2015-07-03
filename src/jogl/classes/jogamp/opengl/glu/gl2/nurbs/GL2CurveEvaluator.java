package jogamp.opengl.glu.gl2.nurbs;
import jogamp.opengl.glu.nurbs.*;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.gl2.GLUgl2;

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
  private final GL2 gl;

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
  @Override
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
  @Override
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
  @Override
  public void map1f(final int type, final float ulo, final float uhi, final int stride, final int order,
                    final CArrayOfFloats ps) {
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
  @Override
  public void enable(final int type) {
    // DONE
    gl.glEnable(type);
  }

  /**
   * Calls glMapGrid1f
   * @param nu steps
   * @param u1 low u
   * @param u2 high u
   */
  @Override
  public void mapgrid1f(final int nu, final float u1, final float u2) {
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
  @Override
  public void mapmesh1f(final int style, final int from, final int to) {
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
        gl.glEvalMesh1(GL2GL3.GL_LINE, from, to);
        break;
      case Backend.N_MESHPOINT:
        gl.glEvalMesh1(GL2GL3.GL_POINT, from, to);
        break;
      }
    }
  }
}
