package jogamp.opengl.glu.gl2.nurbs;
import jogamp.opengl.glu.nurbs.*;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.gl2.GLUgl2;

/**
 * Class rendering surfaces with OpenGL
 * @author Tomas Hrasky
 *
 */
class GL2SurfaceEvaluator implements SurfaceEvaluator {

  /**
   * JOGL OpenGL object
   */
  private final GL2 gl;

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
  @Override
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
  @Override
  public void polymode(final int style) {
    if (!output_triangles) {
      switch (style) {
      default:
      case NurbsConsts.N_MESHFILL:
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
        break;
      case NurbsConsts.N_MESHLINE:
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
        break;
      case NurbsConsts.N_MESHPOINT:
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_POINT);
        break;
      }
    }

  }

  /**
   * Pops all attributes
   */
  @Override
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
  @Override
  public void domain2f(final float ulo, final float uhi, final float vlo, final float vhi) {
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
  @Override
  public void mapgrid2f(final int nu, final float u0, final float u1, final int nv, final float v0, final float v1) {

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
  @Override
  public void mapmesh2f(final int style, final int umin, final int umax, final int vmin, final int vmax) {
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
        gl.glEvalMesh2(GL2GL3.GL_FILL, umin, umax, vmin, vmax);
        break;
      case NurbsConsts.N_MESHLINE:
        gl.glEvalMesh2(GL2GL3.GL_LINE, umin, umax, vmin, vmax);
        break;
      case NurbsConsts.N_MESHPOINT:
        gl.glEvalMesh2(GL2GL3.GL_POINT, umin, umax, vmin, vmax);
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
  @Override
  public void map2f(final int type, final float ulo, final float uhi, final int ustride, final int uorder,
                    final float vlo, final float vhi, final int vstride, final int vorder, final CArrayOfFloats pts) {
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
  @Override
  public void enable(final int type) {
    //DONE
    gl.glEnable(type);
  }
}
