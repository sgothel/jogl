/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
 */

package javax.media.opengl.fixedfunc;

import java.nio.*;

import javax.media.opengl.*;

public interface GLMatrixFunc {

  public static final int GL_MATRIX_MODE = 0x0BA0;
  public static final int GL_MODELVIEW = 0x1700;
  public static final int GL_PROJECTION = 0x1701;
  // public static final int GL_TEXTURE = 0x1702; // Use GL.GL_TEXTURE due to ambiguous GL usage
  public static final int GL_MODELVIEW_MATRIX = 0x0BA6;
  public static final int GL_PROJECTION_MATRIX = 0x0BA7;
  public static final int GL_TEXTURE_MATRIX = 0x0BA8;

  /**
   * glGetFloatv
   * @param pname GL_MODELVIEW_MATRIX, GL_PROJECTION_MATRIX or GL_TEXTURE_MATRIX
   * @param params the FloatBuffer's position remains unchanged,
   *        which is the same behavior than the native JOGL GL impl
   */
  public void glGetFloatv(int pname, java.nio.FloatBuffer params);
  public void glGetFloatv(int pname, float[] params, int params_offset);
  /**
   * glGetIntegerv
   * @param pname GL_MATRIX_MODE
   * @param params the FloatBuffer's position remains unchanged
   *        which is the same behavior than the native JOGL GL impl
   */
  public void glGetIntegerv(int pname, IntBuffer params);
  public void glGetIntegerv(int pname, int[] params, int params_offset);

  /**
   * sets the current matrix
   * @param pname GL_MODELVIEW, GL_PROJECTION or GL.GL_TEXTURE
   */
  public void glMatrixMode(int mode) ;

  public void glPushMatrix();
  public void glPopMatrix();

  public void glLoadIdentity() ;

  /**
   * glLoadMatrixf
   * @param params the FloatBuffer's position remains unchanged,
   *        which is the same behavior than the native JOGL GL impl
   */
  public void glLoadMatrixf(java.nio.FloatBuffer m) ;
  public void glLoadMatrixf(float[] m, int m_offset);

  /**
   * glMultMatrixf
   * @param params the FloatBuffer's position remains unchanged,
   *        which is the same behavior than the native JOGL GL impl
   */
  public void glMultMatrixf(java.nio.FloatBuffer m) ;
  public void glMultMatrixf(float[] m, int m_offset);

  public void glTranslatef(float x, float y, float z) ;

  public void glRotatef(float angle, float x, float y, float z);

  public void glScalef(float x, float y, float z) ;

  public void glOrthof(float left, float right, float bottom, float top, float zNear, float zFar) ;

  public void glFrustumf(float left, float right, float bottom, float top, float zNear, float zFar);

}

