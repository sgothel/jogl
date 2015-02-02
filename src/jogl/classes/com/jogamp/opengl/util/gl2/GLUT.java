/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.jogamp.opengl.util.gl2;

import com.jogamp.opengl.*;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.glu.gl2.*;

/** Subset of the routines provided by the GLUT interface. Note the
    signatures of many of the methods are necessarily different than
    the corresponding C version. A GLUT object must only be used from
    one particular thread at a time. <P>

    Copyright (c) Mark J. Kilgard, 1994, 1997. <P>

    (c) Copyright 1993, Silicon Graphics, Inc. <P>

    ALL RIGHTS RESERVED <P>

    Permission to use, copy, modify, and distribute this software
    for any purpose and without fee is hereby granted, provided
    that the above copyright notice appear in all copies and that
    both the copyright notice and this permission notice appear in
    supporting documentation, and that the name of Silicon
    Graphics, Inc. not be used in advertising or publicity
    pertaining to distribution of the software without specific,
    written prior permission. <P>

    THE MATERIAL EMBODIED ON THIS SOFTWARE IS PROVIDED TO YOU
    "AS-IS" AND WITHOUT WARRANTY OF ANY KIND, EXPRESS, IMPLIED OR
    OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF
    MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  IN NO
    EVENT SHALL SILICON GRAPHICS, INC.  BE LIABLE TO YOU OR ANYONE
    ELSE FOR ANY DIRECT, SPECIAL, INCIDENTAL, INDIRECT OR
    CONSEQUENTIAL DAMAGES OF ANY KIND, OR ANY DAMAGES WHATSOEVER,
    INCLUDING WITHOUT LIMITATION, LOSS OF PROFIT, LOSS OF USE,
    SAVINGS OR REVENUE, OR THE CLAIMS OF THIRD PARTIES, WHETHER OR
    NOT SILICON GRAPHICS, INC.  HAS BEEN ADVISED OF THE POSSIBILITY
    OF SUCH LOSS, HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
    ARISING OUT OF OR IN CONNECTION WITH THE POSSESSION, USE OR
    PERFORMANCE OF THIS SOFTWARE. <P>

    US Government Users Restricted Rights <P>

    Use, duplication, or disclosure by the Government is subject to
    restrictions set forth in FAR 52.227.19(c)(2) or subparagraph
    (c)(1)(ii) of the Rights in Technical Data and Computer
    Software clause at DFARS 252.227-7013 and/or in similar or
    successor clauses in the FAR or the DOD or NASA FAR
    Supplement.  Unpublished-- rights reserved under the copyright
    laws of the United States.  Contractor/manufacturer is Silicon
    Graphics, Inc., 2011 N.  Shoreline Blvd., Mountain View, CA
    94039-7311. <P>

    OpenGL(TM) is a trademark of Silicon Graphics, Inc. <P>
*/

public class GLUT {
  public static final int STROKE_ROMAN = 0;
  public static final int STROKE_MONO_ROMAN = 1;
  public static final int BITMAP_9_BY_15 = 2;
  public static final int BITMAP_8_BY_13 = 3;
  public static final int BITMAP_TIMES_ROMAN_10 = 4;
  public static final int BITMAP_TIMES_ROMAN_24 = 5;
  public static final int BITMAP_HELVETICA_10 = 6;
  public static final int BITMAP_HELVETICA_12 = 7;
  public static final int BITMAP_HELVETICA_18 = 8;

  private final GLUgl2 glu = new GLUgl2();

  //----------------------------------------------------------------------
  // Shapes
  //

  public void glutWireSphere(final double radius, final int slices, final int stacks) {
    quadObjInit(glu);
    glu.gluQuadricDrawStyle(quadObj, GLU.GLU_LINE);
    glu.gluQuadricNormals(quadObj, GLU.GLU_SMOOTH);
    /* If we ever changed/used the texture or orientation state
       of quadObj, we'd need to change it to the defaults here
       with gluQuadricTexture and/or gluQuadricOrientation. */
    glu.gluSphere(quadObj, radius, slices, stacks);
  }

  public void glutSolidSphere(final double radius, final int slices, final int stacks) {
    quadObjInit(glu);
    glu.gluQuadricDrawStyle(quadObj, GLU.GLU_FILL);
    glu.gluQuadricNormals(quadObj, GLU.GLU_SMOOTH);
    /* If we ever changed/used the texture or orientation state
       of quadObj, we'd need to change it to the defaults here
       with gluQuadricTexture and/or gluQuadricOrientation. */
    glu.gluSphere(quadObj, radius, slices, stacks);
  }

  public void glutWireCone(final double base, final double height,
                           final int slices, final int stacks) {
    quadObjInit(glu);
    glu.gluQuadricDrawStyle(quadObj, GLU.GLU_LINE);
    glu.gluQuadricNormals(quadObj, GLU.GLU_SMOOTH);
    /* If we ever changed/used the texture or orientation state
       of quadObj, we'd need to change it to the defaults here
       with gluQuadricTexture and/or gluQuadricOrientation. */
    glu.gluCylinder(quadObj, base, 0.0, height, slices, stacks);
  }

  public void glutSolidCone(final double base, final double height,
                            final int slices, final int stacks) {
    quadObjInit(glu);
    glu.gluQuadricDrawStyle(quadObj, GLU.GLU_FILL);
    glu.gluQuadricNormals(quadObj, GLU.GLU_SMOOTH);
    /* If we ever changed/used the texture or orientation state
       of quadObj, we'd need to change it to the defaults here
       with gluQuadricTexture and/or gluQuadricOrientation. */
    glu.gluCylinder(quadObj, base, 0.0, height, slices, stacks);
  }

  public void glutWireCylinder(final double radius, final double height, final int slices, final int stacks) {
    quadObjInit(glu);
    glu.gluQuadricDrawStyle(quadObj, GLU.GLU_LINE);
    glu.gluQuadricNormals(quadObj, GLU.GLU_SMOOTH);
    /* If we ever changed/used the texture or orientation state
       of quadObj, we'd need to change it to the defaults here
       with gluQuadricTexture and/or gluQuadricOrientation. */
    glu.gluCylinder(quadObj, radius, radius, height, slices, stacks);
  }

  public void glutSolidCylinder(final double radius, final double height, final int slices, final int stacks) {
    final GL2 gl = GLUgl2.getCurrentGL2();

    // Prepare table of points for drawing end caps
    final double [] x = new double[slices];
    final double [] y = new double[slices];
    final double angleDelta = Math.PI * 2 / slices;
    double angle = 0;
    for (int i = 0 ; i < slices ; i ++) {
      angle = i * angleDelta;
      x[i] = Math.cos(angle) * radius;
      y[i] = Math.sin(angle) * radius;
    }

    // Draw bottom cap
    gl.glBegin(GL.GL_TRIANGLE_FAN);
    gl.glNormal3d(0,0,-1);
    gl.glVertex3d(0,0,0);
    for (int i = 0 ; i < slices ; i ++) {
      gl.glVertex3d(x[i], y[i], 0);
    }
    gl.glVertex3d(x[0], y[0], 0);
    gl.glEnd();

    // Draw top cap
    gl.glBegin(GL.GL_TRIANGLE_FAN);
    gl.glNormal3d(0,0,1);
    gl.glVertex3d(0,0,height);
    for (int i = 0 ; i < slices ; i ++) {
      gl.glVertex3d(x[i], y[i], height);
    }
    gl.glVertex3d(x[0], y[0], height);
    gl.glEnd();

    // Draw walls
    quadObjInit(glu);
    glu.gluQuadricDrawStyle(quadObj, GLU.GLU_FILL);
    glu.gluQuadricNormals(quadObj, GLU.GLU_SMOOTH);
    /* If we ever changed/used the texture or orientation state
       of quadObj, we'd need to change it to the defaults here
       with gluQuadricTexture and/or gluQuadricOrientation. */
    glu.gluCylinder(quadObj, radius, radius, height, slices, stacks);
  }

  public void glutWireCube(final float size) {
    drawBox(GLUgl2.getCurrentGL2(), size, GL.GL_LINE_LOOP);
  }

  public void glutSolidCube(final float size) {
    drawBox(GLUgl2.getCurrentGL2(), size, GL2GL3.GL_QUADS);
  }

  public void glutWireTorus(final double innerRadius, final double outerRadius,
                            final int nsides, final int rings) {
    final GL2 gl = GLUgl2.getCurrentGL2();
    gl.glPushAttrib(GL2.GL_POLYGON_BIT);
    gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
    doughnut(gl, innerRadius, outerRadius, nsides, rings);
    gl.glPopAttrib();
  }

  public void glutSolidTorus(final double innerRadius, final double outerRadius,
                             final int nsides, final int rings) {
    doughnut(GLUgl2.getCurrentGL2(), innerRadius, outerRadius, nsides, rings);
  }

  public void glutWireDodecahedron() {
    dodecahedron(GLUgl2.getCurrentGL2(), GL.GL_LINE_LOOP);
  }

  public void glutSolidDodecahedron() {
    dodecahedron(GLUgl2.getCurrentGL2(), GL.GL_TRIANGLE_FAN);
  }

  public void glutWireOctahedron() {
    octahedron(GLUgl2.getCurrentGL2(), GL.GL_LINE_LOOP);
  }

  public void glutSolidOctahedron() {
    octahedron(GLUgl2.getCurrentGL2(), GL.GL_TRIANGLES);
  }

  public void glutWireIcosahedron() {
    icosahedron(GLUgl2.getCurrentGL2(), GL.GL_LINE_LOOP);
  }

  public void glutSolidIcosahedron() {
    icosahedron(GLUgl2.getCurrentGL2(), GL.GL_TRIANGLES);
  }

  public void glutWireTetrahedron() {
    tetrahedron(GLUgl2.getCurrentGL2(), GL.GL_LINE_LOOP);
  }

  public void glutSolidTetrahedron() {
    tetrahedron(GLUgl2.getCurrentGL2(), GL.GL_TRIANGLES);
  }

/**
   * Renders the teapot as a solid shape of the specified size. The teapot is
   * created in a way that replicates the C GLUT implementation.
   *
   * @param scale
   *        the factor by which to scale the teapot
   */
  public void glutSolidTeapot(final double scale) {
    glutSolidTeapot(scale, true);
  }

  /**
   * Renders the teapot as a solid shape of the specified size. The teapot can
   * either be created in a way that is backward-compatible with the standard
   * C glut library (i.e. broken), or in a more pleasing way (i.e. with
   * surfaces whose front-faces point outwards and standing on the z=0 plane,
   * instead of the y=-1 plane). Both surface normals and texture coordinates
   * for the teapot are generated. The teapot is generated with OpenGL
   * evaluators.
   *
   * @param scale
   *        the factor by which to scale the teapot
   * @param cStyle
   *        whether to create the teapot in exactly the same way as in the C
   *        implementation of GLUT
   */
  public void glutSolidTeapot(final double scale, final boolean cStyle) {
    teapot(GLUgl2.getCurrentGL2(), 14, scale, GL2GL3.GL_FILL, cStyle);
  }

  /**
   * Renders the teapot as a wireframe shape of the specified size. The teapot
   * is created in a way that replicates the C GLUT implementation.
   *
   * @param scale
   *        the factor by which to scale the teapot
   */
  public void glutWireTeapot(final double scale) {
    glutWireTeapot(scale, true);
  }

  /**
   * Renders the teapot as a wireframe shape of the specified size. The teapot
   * can either be created in a way that is backward-compatible with the
   * standard C glut library (i.e. broken), or in a more pleasing way (i.e.
   * with surfaces whose front-faces point outwards and standing on the z=0
   * plane, instead of the y=-1 plane). Both surface normals and texture
   * coordinates for the teapot are generated. The teapot is generated with
   * OpenGL evaluators.
   *
   * @param scale
   *        the factor by which to scale the teapot
   * @param cStyle
   *        whether to create the teapot in exactly the same way as in the C
   *        implementation of GLUT
   */
  public void glutWireTeapot(final double scale, final boolean cStyle) {
    teapot(GLUgl2.getCurrentGL2(), 10, scale, GL2GL3.GL_LINE, cStyle);
  }

  //----------------------------------------------------------------------
  // Fonts
  //

  public void glutBitmapCharacter(final int font, final char character) {
    final GL2 gl = GLUgl2.getCurrentGL2();
    final int[] swapbytes  = new int[1];
    final int[] lsbfirst   = new int[1];
    final int[] rowlength  = new int[1];
    final int[] skiprows   = new int[1];
    final int[] skippixels = new int[1];
    final int[] alignment  = new int[1];
    beginBitmap(gl,
                swapbytes,
                lsbfirst,
                rowlength,
                skiprows,
                skippixels,
                alignment);
    bitmapCharacterImpl(gl, font, character);
    endBitmap(gl,
              swapbytes,
              lsbfirst,
              rowlength,
              skiprows,
              skippixels,
              alignment);
  }

  public void glutBitmapString   (final int font, final String string) {
    final GL2 gl = GLUgl2.getCurrentGL2();
    final int[] swapbytes  = new int[1];
    final int[] lsbfirst   = new int[1];
    final int[] rowlength  = new int[1];
    final int[] skiprows   = new int[1];
    final int[] skippixels = new int[1];
    final int[] alignment  = new int[1];
    beginBitmap(gl,
                swapbytes,
                lsbfirst,
                rowlength,
                skiprows,
                skippixels,
                alignment);
    final int len = string.length();
    for (int i = 0; i < len; i++) {
      bitmapCharacterImpl(gl, font, string.charAt(i));
    }
    endBitmap(gl,
              swapbytes,
              lsbfirst,
              rowlength,
              skiprows,
              skippixels,
              alignment);
  }

  public int  glutBitmapWidth    (final int font, final char character) {
    final BitmapFontRec fontinfo = getBitmapFont(font);
    final int c = character & 0xFFFF;
    if (c < fontinfo.first || c >= fontinfo.first + fontinfo.num_chars)
      return 0;
    final BitmapCharRec ch = fontinfo.ch[c - fontinfo.first];
    if (ch != null)
      return (int) ch.advance;
    else
      return 0;
  }

  public void glutStrokeCharacter(final int font, final char character) {
    final GL2 gl = GLUgl2.getCurrentGL2();
    final StrokeFontRec fontinfo = getStrokeFont(font);
    final int c = character & 0xFFFF;
    if (c < 0 || c >= fontinfo.num_chars)
      return;
    final StrokeCharRec ch = fontinfo.ch[c];
    if (ch != null) {
      for (int i = 0; i < ch.num_strokes; i++) {
        final StrokeRec stroke = ch.stroke[i];
        gl.glBegin(GL.GL_LINE_STRIP);
        for (int j = 0; j < stroke.num_coords; j++) {
          final CoordRec coord = stroke.coord[j];
          gl.glVertex2f(coord.x, coord.y);
        }
        gl.glEnd();
      }
      gl.glTranslatef(ch.right, 0.0f, 0.0f);
    }
  }

  public void glutStrokeString(final int font, final String string) {
    final GL2 gl = GLUgl2.getCurrentGL2();
    final StrokeFontRec fontinfo = getStrokeFont(font);
    final int len = string.length();
    for (int pos = 0; pos < len; pos++) {
      final int c = string.charAt(pos) & 0xFFFF;
      if (c < 0 || c >= fontinfo.num_chars)
        continue;
      final StrokeCharRec ch = fontinfo.ch[c];
      if (ch != null) {
        for (int i = 0; i < ch.num_strokes; i++) {
          final StrokeRec stroke = ch.stroke[i];
          gl.glBegin(GL.GL_LINE_STRIP);
          for (int j = 0; j < stroke.num_coords; j++) {
            final CoordRec coord = stroke.coord[j];
            gl.glVertex2f(coord.x, coord.y);
          }
          gl.glEnd();
        }
        gl.glTranslatef(ch.right, 0.0f, 0.0f);
      }
    }
  }

  public int  glutStrokeWidth    (final int font, final char character) {
    return (int) glutStrokeWidthf(font, character);
  }

  public float glutStrokeWidthf   (final int font, final char character) {
    final StrokeFontRec fontinfo = getStrokeFont(font);
    final int c = character & 0xFFFF;
    if (c < 0 || c >= fontinfo.num_chars)
      return 0;
    final StrokeCharRec ch = fontinfo.ch[c];
    if (ch != null)
      return ch.right;
    else
      return 0;
  }

  public int  glutBitmapLength   (final int font, final String string) {
    final BitmapFontRec fontinfo = getBitmapFont(font);
    int length = 0;
    final int len = string.length();
    for (int pos = 0; pos < len; pos++) {
      final int c = string.charAt(pos) & 0xFFFF;
      if (c >= fontinfo.first && c < fontinfo.first + fontinfo.num_chars) {
        final BitmapCharRec ch = fontinfo.ch[c - fontinfo.first];
        if (ch != null)
          length += ch.advance;
      }
    }
    return length;
  }

  public int  glutStrokeLength   (final int font, final String string) {
    return (int) glutStrokeLengthf(font, string);
  }

  public float glutStrokeLengthf  (final int font, final String string) {
    final StrokeFontRec fontinfo = getStrokeFont(font);
    float length = 0;
    final int len = string.length();
    for (int i = 0; i < len; i++) {
      final char c = string.charAt(i);
      if (c >= 0 && c < fontinfo.num_chars) {
        final StrokeCharRec ch = fontinfo.ch[c];
        if (ch != null)
          length += ch.right;
      }
    }
    return length;
  }

  /**
     This function draws a wireframe dodecahedron whose
     facets are rhombic and
     whose vertices are at unit radius.
     No facet lies normal to any coordinate axes.
     The polyhedron is centered at the origin.
  */
  public void glutWireRhombicDodecahedron() {
    final GL2 gl = GLUgl2.getCurrentGL2();
    for( int i = 0; i < 12; i++ ) {
      gl.glBegin( GL.GL_LINE_LOOP );
      gl.glNormal3dv( rdod_n[ i ],0 );
      gl.glVertex3dv( rdod_r[ rdod_v[ i ][ 0 ] ],0 );
      gl.glVertex3dv( rdod_r[ rdod_v[ i ][ 1 ] ],0 );
      gl.glVertex3dv( rdod_r[ rdod_v[ i ][ 2 ] ],0 );
      gl.glVertex3dv( rdod_r[ rdod_v[ i ][ 3 ] ],0 );
      gl.glEnd( );
    }
  }

  /**
   This function draws a solid-shaded dodecahedron
   whose facets are rhombic and
   whose vertices are at unit radius.
   No facet lies normal to any coordinate axes.
   The polyhedron is centered at the origin.
   */
  public void glutSolidRhombicDodecahedron() {
    final GL2 gl = GLUgl2.getCurrentGL2();
    gl.glBegin( GL2GL3.GL_QUADS );
    for( int i = 0; i < 12; i++ ) {
      gl.glNormal3dv( rdod_n[ i ],0 );
      gl.glVertex3dv( rdod_r[ rdod_v[ i ][ 0 ] ],0 );
      gl.glVertex3dv( rdod_r[ rdod_v[ i ][ 1 ] ],0 );
      gl.glVertex3dv( rdod_r[ rdod_v[ i ][ 2 ] ],0 );
      gl.glVertex3dv( rdod_r[ rdod_v[ i ][ 3 ] ],0 );
    }
    gl.glEnd( );
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  //----------------------------------------------------------------------
  // Shape implementation
  //

  private GLUquadric quadObj;
  private void quadObjInit(final GLUgl2 glu) {
    if (quadObj == null) {
      quadObj = glu.gluNewQuadric();
    }
    if (quadObj == null) {
      throw new GLException("Out of memory");
    }
  }

  private static void doughnut(final GL2 gl, final double r, final double R, final int nsides, final int rings) {
    int i, j;
    float theta, phi, theta1;
    float cosTheta, sinTheta;
    float cosTheta1, sinTheta1;
    float ringDelta, sideDelta;

    ringDelta = (float) (2.0 * Math.PI / rings);
    sideDelta = (float) (2.0 * Math.PI / nsides);

    theta = 0.0f;
    cosTheta = 1.0f;
    sinTheta = 0.0f;
    for (i = rings - 1; i >= 0; i--) {
      theta1 = theta + ringDelta;
      cosTheta1 = (float) Math.cos(theta1);
      sinTheta1 = (float) Math.sin(theta1);
      gl.glBegin(GL2.GL_QUAD_STRIP);
      phi = 0.0f;
      for (j = nsides; j >= 0; j--) {
        float cosPhi, sinPhi, dist;

        phi += sideDelta;
        cosPhi = (float) Math.cos(phi);
        sinPhi = (float) Math.sin(phi);
        dist = (float) (R + r * cosPhi);

        gl.glNormal3f(cosTheta1 * cosPhi, -sinTheta1 * cosPhi, sinPhi);
        gl.glVertex3f(cosTheta1 * dist,   -sinTheta1 * dist,   (float) r * sinPhi);
        gl.glNormal3f(cosTheta  * cosPhi, -sinTheta  * cosPhi, sinPhi);
        gl.glVertex3f(cosTheta  * dist,   -sinTheta  * dist,   (float) r * sinPhi);
      }
      gl.glEnd();
      theta = theta1;
      cosTheta = cosTheta1;
      sinTheta = sinTheta1;
    }
  }

  private static float[][] boxVertices;
  private static final float[][] boxNormals = {
    {-1.0f, 0.0f, 0.0f},
    {0.0f, 1.0f, 0.0f},
    {1.0f, 0.0f, 0.0f},
    {0.0f, -1.0f, 0.0f},
    {0.0f, 0.0f, 1.0f},
    {0.0f, 0.0f, -1.0f}
  };
  private static final int[][] boxFaces = {
    {0, 1, 2, 3},
    {3, 2, 6, 7},
    {7, 6, 5, 4},
    {4, 5, 1, 0},
    {5, 6, 2, 1},
    {7, 4, 0, 3}
  };
  private void drawBox(final GL2 gl, final float size, final int type) {
    if (boxVertices == null) {
      final float[][] v = new float[8][];
      for (int i = 0; i < 8; i++) {
        v[i] = new float[3];
      }
      v[0][0] = v[1][0] = v[2][0] = v[3][0] = -0.5f;
      v[4][0] = v[5][0] = v[6][0] = v[7][0] =  0.5f;
      v[0][1] = v[1][1] = v[4][1] = v[5][1] = -0.5f;
      v[2][1] = v[3][1] = v[6][1] = v[7][1] =  0.5f;
      v[0][2] = v[3][2] = v[4][2] = v[7][2] = -0.5f;
      v[1][2] = v[2][2] = v[5][2] = v[6][2] =  0.5f;
      boxVertices = v;
    }
    final float[][] v = boxVertices;
    final float[][] n = boxNormals;
    final int[][] faces = boxFaces;
    for (int i = 5; i >= 0; i--) {
      gl.glBegin(type);
      gl.glNormal3fv(n[i], 0);
      float[] vt = v[faces[i][0]];
      gl.glVertex3f(vt[0] * size, vt[1] * size, vt[2] * size);
      vt = v[faces[i][1]];
      gl.glVertex3f(vt[0] * size, vt[1] * size, vt[2] * size);
      vt = v[faces[i][2]];
      gl.glVertex3f(vt[0] * size, vt[1] * size, vt[2] * size);
      vt = v[faces[i][3]];
      gl.glVertex3f(vt[0] * size, vt[1] * size, vt[2] * size);
      gl.glEnd();
    }
  }

  private float[][] dodec;

  private void initDodecahedron() {
    dodec = new float[20][];
    for (int i = 0; i < dodec.length; i++) {
      dodec[i] = new float[3];
    }

    float alpha, beta;

    alpha = (float) Math.sqrt(2.0f / (3.0f + Math.sqrt(5.0)));
    beta = 1.0f + (float) Math.sqrt(6.0 / (3.0 + Math.sqrt(5.0)) -
                                    2.0 + 2.0 * Math.sqrt(2.0 / (3.0 + Math.sqrt(5.0))));
    dodec[0][0] = -alpha; dodec[0][1] = 0; dodec[0][2] = beta;
    dodec[1][0] = alpha; dodec[1][1] = 0; dodec[1][2] = beta;
    dodec[2][0] = -1; dodec[2][1] = -1; dodec[2][2] = -1;
    dodec[3][0] = -1; dodec[3][1] = -1; dodec[3][2] = 1;
    dodec[4][0] = -1; dodec[4][1] = 1; dodec[4][2] = -1;
    dodec[5][0] = -1; dodec[5][1] = 1; dodec[5][2] = 1;
    dodec[6][0] = 1; dodec[6][1] = -1; dodec[6][2] = -1;
    dodec[7][0] = 1; dodec[7][1] = -1; dodec[7][2] = 1;
    dodec[8][0] = 1; dodec[8][1] = 1; dodec[8][2] = -1;
    dodec[9][0] = 1; dodec[9][1] = 1; dodec[9][2] = 1;
    dodec[10][0] = beta; dodec[10][1] = alpha; dodec[10][2] = 0;
    dodec[11][0] = beta; dodec[11][1] = -alpha; dodec[11][2] = 0;
    dodec[12][0] = -beta; dodec[12][1] = alpha; dodec[12][2] = 0;
    dodec[13][0] = -beta; dodec[13][1] = -alpha; dodec[13][2] = 0;
    dodec[14][0] = -alpha; dodec[14][1] = 0; dodec[14][2] = -beta;
    dodec[15][0] = alpha; dodec[15][1] = 0; dodec[15][2] = -beta;
    dodec[16][0] = 0; dodec[16][1] = beta; dodec[16][2] = alpha;
    dodec[17][0] = 0; dodec[17][1] = beta; dodec[17][2] = -alpha;
    dodec[18][0] = 0; dodec[18][1] = -beta; dodec[18][2] = alpha;
    dodec[19][0] = 0; dodec[19][1] = -beta; dodec[19][2] = -alpha;
  }

  private static void diff3(final float[] a, final float[] b, final float[] c) {
    c[0] = a[0] - b[0];
    c[1] = a[1] - b[1];
    c[2] = a[2] - b[2];
  }

  private static void crossprod(final float[] v1, final float[] v2, final float[] prod) {
    final float[] p = new float[3];         /* in case prod == v1 or v2 */

    p[0] = v1[1] * v2[2] - v2[1] * v1[2];
    p[1] = v1[2] * v2[0] - v2[2] * v1[0];
    p[2] = v1[0] * v2[1] - v2[0] * v1[1];
    prod[0] = p[0];
    prod[1] = p[1];
    prod[2] = p[2];
  }

  private static void normalize(final float[] v) {
    float d;

    d = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    if (d == 0.0) {
      v[0] = d = 1.0f;
    }
    d = 1 / d;
    v[0] *= d;
    v[1] *= d;
    v[2] *= d;
  }

  private void pentagon(final GL2 gl, final int a, final int b, final int c, final int d, final int e, final int shadeType) {
    final float[] n0 = new float[3];
    final float[] d1 = new float[3];
    final float[] d2 = new float[3];

    diff3(dodec[a], dodec[b], d1);
    diff3(dodec[b], dodec[c], d2);
    crossprod(d1, d2, n0);
    normalize(n0);

    gl.glBegin(shadeType);
    gl.glNormal3fv(n0, 0);
    gl.glVertex3fv(dodec[a], 0);
    gl.glVertex3fv(dodec[b], 0);
    gl.glVertex3fv(dodec[c], 0);
    gl.glVertex3fv(dodec[d], 0);
    gl.glVertex3fv(dodec[e], 0);
    gl.glEnd();
  }

  private void dodecahedron(final GL2 gl, final int type) {
    if (dodec == null) {
      initDodecahedron();
    }
    pentagon(gl, 0, 1, 9, 16, 5, type);
    pentagon(gl, 1, 0, 3, 18, 7, type);
    pentagon(gl, 1, 7, 11, 10, 9, type);
    pentagon(gl, 11, 7, 18, 19, 6, type);
    pentagon(gl, 8, 17, 16, 9, 10, type);
    pentagon(gl, 2, 14, 15, 6, 19, type);
    pentagon(gl, 2, 13, 12, 4, 14, type);
    pentagon(gl, 2, 19, 18, 3, 13, type);
    pentagon(gl, 3, 0, 5, 12, 13, type);
    pentagon(gl, 6, 15, 8, 10, 11, type);
    pentagon(gl, 4, 17, 8, 15, 14, type);
    pentagon(gl, 4, 12, 5, 16, 17, type);
  }

  private static void recorditem(final GL2 gl, final float[] n1, final float[] n2, final float[] n3, final int shadeType) {
    final float[] q0 = new float[3];
    final float[] q1 = new float[3];

    diff3(n1, n2, q0);
    diff3(n2, n3, q1);
    crossprod(q0, q1, q1);
    normalize(q1);

    gl.glBegin(shadeType);
    gl.glNormal3fv(q1, 0);
    gl.glVertex3fv(n1, 0);
    gl.glVertex3fv(n2, 0);
    gl.glVertex3fv(n3, 0);
    gl.glEnd();
  }

  private static void subdivide(final GL2 gl, final float[] v0, final float[] v1, final float[] v2, final int shadeType) {
    int depth;
    final float[] w0 = new float[3];
    final float[] w1 = new float[3];
    final float[] w2 = new float[3];
    float l;
    int i, j, k, n;

    depth = 1;
    for (i = 0; i < depth; i++) {
      for (j = 0; i + j < depth; j++) {
        k = depth - i - j;
        for (n = 0; n < 3; n++) {
          w0[n] = (i * v0[n] + j * v1[n] + k * v2[n]) / depth;
          w1[n] = ((i + 1) * v0[n] + j * v1[n] + (k - 1) * v2[n])
            / depth;
          w2[n] = (i * v0[n] + (j + 1) * v1[n] + (k - 1) * v2[n])
            / depth;
        }
        l = (float) Math.sqrt(w0[0] * w0[0] + w0[1] * w0[1] + w0[2] * w0[2]);
        w0[0] /= l;
        w0[1] /= l;
        w0[2] /= l;
        l = (float) Math.sqrt(w1[0] * w1[0] + w1[1] * w1[1] + w1[2] * w1[2]);
        w1[0] /= l;
        w1[1] /= l;
        w1[2] /= l;
        l = (float) Math.sqrt(w2[0] * w2[0] + w2[1] * w2[1] + w2[2] * w2[2]);
        w2[0] /= l;
        w2[1] /= l;
        w2[2] /= l;
        recorditem(gl, w1, w0, w2, shadeType);
      }
    }
  }

  private static void drawtriangle(final GL2 gl, final int i, final float[][] data, final int[][] ndx, final int shadeType) {
    final float[] x0 = data[ndx[i][0]];
    final float[] x1 = data[ndx[i][1]];
    final float[] x2 = data[ndx[i][2]];
    subdivide(gl, x0, x1, x2, shadeType);
  }

  /* octahedron data: The octahedron produced is centered at the
     origin and has radius 1.0 */
  private static final float[][] odata =
  {
    {1.0f, 0.0f, 0.0f},
    {-1.0f, 0.0f, 0.0f},
    {0.0f, 1.0f, 0.0f},
    {0.0f, -1.0f, 0.0f},
    {0.0f, 0.0f, 1.0f},
    {0.0f, 0.0f, -1.0f}
  };

  private static final int[][] ondex =
  {
    {0, 4, 2},
    {1, 2, 4},
    {0, 3, 4},
    {1, 4, 3},
    {0, 2, 5},
    {1, 5, 2},
    {0, 5, 3},
    {1, 3, 5}
  };

  private static void octahedron(final GL2 gl, final int shadeType) {
    int i;

    for (i = 7; i >= 0; i--) {
      drawtriangle(gl, i, odata, ondex, shadeType);
    }
  }

  /* icosahedron data: These numbers are rigged to make an
     icosahedron of radius 1.0 */

  private static final float X = .525731112119133606f;
  private static final float Z = .850650808352039932f;

  private static final float[][] idata =
  {
    {-X, 0, Z},
    {X, 0, Z},
    {-X, 0, -Z},
    {X, 0, -Z},
    {0, Z, X},
    {0, Z, -X},
    {0, -Z, X},
    {0, -Z, -X},
    {Z, X, 0},
    {-Z, X, 0},
    {Z, -X, 0},
    {-Z, -X, 0}
  };

  private static final int[][] index =
  {
    {0, 4, 1},
    {0, 9, 4},
    {9, 5, 4},
    {4, 5, 8},
    {4, 8, 1},
    {8, 10, 1},
    {8, 3, 10},
    {5, 3, 8},
    {5, 2, 3},
    {2, 7, 3},
    {7, 10, 3},
    {7, 6, 10},
    {7, 11, 6},
    {11, 0, 6},
    {0, 1, 6},
    {6, 1, 10},
    {9, 0, 11},
    {9, 11, 2},
    {9, 2, 5},
    {7, 2, 11},
  };

  private static void icosahedron(final GL2 gl, final int shadeType) {
    int i;

    for (i = 19; i >= 0; i--) {
      drawtriangle(gl, i, idata, index, shadeType);
    }
  }

  /* rhombic dodecahedron data: */

  private static final double rdod_r[][] =
  {
    { 0.0, 0.0, 1.0 },
    {  0.707106781187,  0.000000000000,  0.5 },
    {  0.000000000000,  0.707106781187,  0.5 },
    { -0.707106781187,  0.000000000000,  0.5 },
    {  0.000000000000, -0.707106781187,  0.5 },
    {  0.707106781187,  0.707106781187,  0.0 },
    { -0.707106781187,  0.707106781187,  0.0 },
    { -0.707106781187, -0.707106781187,  0.0 },
    {  0.707106781187, -0.707106781187,  0.0 },
    {  0.707106781187,  0.000000000000, -0.5 },
    {  0.000000000000,  0.707106781187, -0.5 },
    { -0.707106781187,  0.000000000000, -0.5 },
    {  0.000000000000, -0.707106781187, -0.5 },
    {  0.0, 0.0, -1.0 }
  };

  private static final int rdod_v[][] =
  {
    { 0,  1,  5,  2 },
    { 0,  2,  6,  3 },
    { 0,  3,  7,  4 },
    { 0,  4,  8,  1 },
    { 5, 10,  6,  2 },
    { 6, 11,  7,  3 },
    { 7, 12,  8,  4 },
    { 8,  9,  5,  1 },
    { 5,  9, 13, 10 },
    { 6, 10, 13, 11 },
    { 7, 11, 13, 12 },
    { 8, 12, 13,  9 }
  };

  private static final double rdod_n[][] =
  {
    {  0.353553390594,  0.353553390594,  0.5 },
    { -0.353553390594,  0.353553390594,  0.5 },
    { -0.353553390594, -0.353553390594,  0.5 },
    {  0.353553390594, -0.353553390594,  0.5 },
    {  0.000000000000,  1.000000000000,  0.0 },
    { -1.000000000000,  0.000000000000,  0.0 },
    {  0.000000000000, -1.000000000000,  0.0 },
    {  1.000000000000,  0.000000000000,  0.0 },
    {  0.353553390594,  0.353553390594, -0.5 },
    { -0.353553390594,  0.353553390594, -0.5 },
    { -0.353553390594, -0.353553390594, -0.5 },
    {  0.353553390594, -0.353553390594, -0.5 }
  };

  /* tetrahedron data: */

  private static final float T = 1.73205080756887729f;

  private static final float[][] tdata =
  {
    {T, T, T},
    {T, -T, -T},
    {-T, T, -T},
    {-T, -T, T}
  };

  private static final int[][] tndex =
  {
    {0, 1, 3},
    {2, 1, 0},
    {3, 2, 0},
    {1, 2, 3}
  };

  private static final void tetrahedron(final GL2 gl, final int shadeType) {
    for (int i = 3; i >= 0; i--)
      drawtriangle(gl, i, tdata, tndex, shadeType);
  }

  // Teapot implementation (a modified port of glut_teapot.c)
  //
  // Rim, body, lid, and bottom data must be reflected in x and
  // y; handle and spout data across the y axis only.
  private static final int[][] teapotPatchData = {
    /* rim */
    {102, 103, 104, 105, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
    /* body */
    {12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27},
    {24, 25, 26, 27, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40},
    /* lid */
    {96, 96, 96, 96, 97, 98, 99, 100, 101, 101, 101, 101, 0, 1, 2, 3,},
    {0, 1, 2, 3, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117},
    /* bottom */
    {118, 118, 118, 118, 124, 122, 119, 121, 123, 126, 125, 120, 40, 39, 38, 37},
    /* handle */
    {41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56},
    {53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 28, 65, 66, 67},
    /* spout */
    {68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83},
    {80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95}
  };
  private static final float[][] teapotCPData = {
    {0.2f, 0f, 2.7f},
    {0.2f, -0.112f, 2.7f},
    {0.112f, -0.2f, 2.7f},
    {0f, -0.2f, 2.7f},
    {1.3375f, 0f, 2.53125f},
    {1.3375f, -0.749f, 2.53125f},
    {0.749f, -1.3375f, 2.53125f},
    {0f, -1.3375f, 2.53125f},
    {1.4375f, 0f, 2.53125f},
    {1.4375f, -0.805f, 2.53125f},
    {0.805f, -1.4375f, 2.53125f},
    {0f, -1.4375f, 2.53125f},
    {1.5f, 0f, 2.4f},
    {1.5f, -0.84f, 2.4f},
    {0.84f, -1.5f, 2.4f},
    {0f, -1.5f, 2.4f},
    {1.75f, 0f, 1.875f},
    {1.75f, -0.98f, 1.875f},
    {0.98f, -1.75f, 1.875f},
    {0f, -1.75f, 1.875f},
    {2f, 0f, 1.35f},
    {2f, -1.12f, 1.35f},
    {1.12f, -2f, 1.35f},
    {0f, -2f, 1.35f},
    {2f, 0f, 0.9f},
    {2f, -1.12f, 0.9f},
    {1.12f, -2f, 0.9f},
    {0f, -2f, 0.9f},
    {-2f, 0f, 0.9f},
    {2f, 0f, 0.45f},
    {2f, -1.12f, 0.45f},
    {1.12f, -2f, 0.45f},
    {0f, -2f, 0.45f},
    {1.5f, 0f, 0.225f},
    {1.5f, -0.84f, 0.225f},
    {0.84f, -1.5f, 0.225f},
    {0f, -1.5f, 0.225f},
    {1.5f, 0f, 0.15f},
    {1.5f, -0.84f, 0.15f},
    {0.84f, -1.5f, 0.15f},
    {0f, -1.5f, 0.15f},
    {-1.6f, 0f, 2.025f},
    {-1.6f, -0.3f, 2.025f},
    {-1.5f, -0.3f, 2.25f},
    {-1.5f, 0f, 2.25f},
    {-2.3f, 0f, 2.025f},
    {-2.3f, -0.3f, 2.025f},
    {-2.5f, -0.3f, 2.25f},
    {-2.5f, 0f, 2.25f},
    {-2.7f, 0f, 2.025f},
    {-2.7f, -0.3f, 2.025f},
    {-3f, -0.3f, 2.25f},
    {-3f, 0f, 2.25f},
    {-2.7f, 0f, 1.8f},
    {-2.7f, -0.3f, 1.8f},
    {-3f, -0.3f, 1.8f},
    {-3f, 0f, 1.8f},
    {-2.7f, 0f, 1.575f},
    {-2.7f, -0.3f, 1.575f},
    {-3f, -0.3f, 1.35f},
    {-3f, 0f, 1.35f},
    {-2.5f, 0f, 1.125f},
    {-2.5f, -0.3f, 1.125f},
    {-2.65f, -0.3f, 0.9375f},
    {-2.65f, 0f, 0.9375f},
    {-2f, -0.3f, 0.9f},
    {-1.9f, -0.3f, 0.6f},
    {-1.9f, 0f, 0.6f},
    {1.7f, 0f, 1.425f},
    {1.7f, -0.66f, 1.425f},
    {1.7f, -0.66f, 0.6f},
    {1.7f, 0f, 0.6f},
    {2.6f, 0f, 1.425f},
    {2.6f, -0.66f, 1.425f},
    {3.1f, -0.66f, 0.825f},
    {3.1f, 0f, 0.825f},
    {2.3f, 0f, 2.1f},
    {2.3f, -0.25f, 2.1f},
    {2.4f, -0.25f, 2.025f},
    {2.4f, 0f, 2.025f},
    {2.7f, 0f, 2.4f},
    {2.7f, -0.25f, 2.4f},
    {3.3f, -0.25f, 2.4f},
    {3.3f, 0f, 2.4f},
    {2.8f, 0f, 2.475f},
    {2.8f, -0.25f, 2.475f},
    {3.525f, -0.25f, 2.49375f},
    {3.525f, 0f, 2.49375f},
    {2.9f, 0f, 2.475f},
    {2.9f, -0.15f, 2.475f},
    {3.45f, -0.15f, 2.5125f},
    {3.45f, 0f, 2.5125f},
    {2.8f, 0f, 2.4f},
    {2.8f, -0.15f, 2.4f},
    {3.2f, -0.15f, 2.4f},
    {3.2f, 0f, 2.4f},
    {0f, 0f, 3.15f},
    {0.8f, 0f, 3.15f},
    {0.8f, -0.45f, 3.15f},
    {0.45f, -0.8f, 3.15f},
    {0f, -0.8f, 3.15f},
    {0f, 0f, 2.85f},
    {1.4f, 0f, 2.4f},
    {1.4f, -0.784f, 2.4f},
    {0.784f, -1.4f, 2.4f},
    {0f, -1.4f, 2.4f},
    {0.4f, 0f, 2.55f},
    {0.4f, -0.224f, 2.55f},
    {0.224f, -0.4f, 2.55f},
    {0f, -0.4f, 2.55f},
    {1.3f, 0f, 2.55f},
    {1.3f, -0.728f, 2.55f},
    {0.728f, -1.3f, 2.55f},
    {0f, -1.3f, 2.55f},
    {1.3f, 0f, 2.4f},
    {1.3f, -0.728f, 2.4f},
    {0.728f, -1.3f, 2.4f},
    {0f, -1.3f, 2.4f},
    {0f, 0f, 0f},
    {1.425f, -0.798f, 0f},
    {1.5f, 0f, 0.075f},
    {1.425f, 0f, 0f},
    {0.798f, -1.425f, 0f},
    {0f, -1.5f, 0.075f},
    {0f, -1.425f, 0f},
    {1.5f, -0.84f, 0.075f},
    {0.84f, -1.5f, 0.075f}
  };
  // Since GL2.glMap2f expects a packed array of floats, we must convert
  // from a 3-dimensional array to a 1-dimensional array
  private static final float[] teapotTex = {
    0, 0, 1, 0, 0, 1, 1, 1
  };

  private static void teapot(final GL2 gl,
                             final int grid,
                             final double scale,
                             final int type,
                             final boolean backCompatible)
  {
    // As mentioned above, GL2.glMap2f expects a packed array of floats
    final float[] p = new float[4*4*3];
    final float[] q = new float[4*4*3];
    final float[] r = new float[4*4*3];
    final float[] s = new float[4*4*3];
    int i, j, k, l;

    gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_EVAL_BIT | GL2.GL_POLYGON_BIT);
    gl.glEnable(GL2.GL_AUTO_NORMAL);
    gl.glEnable(GLLightingFunc.GL_NORMALIZE);
    gl.glEnable(GL2.GL_MAP2_VERTEX_3);
    gl.glEnable(GL2.GL_MAP2_TEXTURE_COORD_2);
    gl.glPushMatrix();
    if (!backCompatible) {
      // The time has come to have the teapot no longer be inside out
      gl.glFrontFace(GL.GL_CW);
      gl.glScaled(0.5*scale, 0.5*scale, 0.5*scale);
    } else {
      // We want the teapot in it's backward compatible position and
      // orientation
      gl.glRotatef(270.0f, 1, 0, 0);
      gl.glScalef((float)(0.5 * scale),
                  (float)(0.5 * scale),
                  (float)(0.5 * scale));
      gl.glTranslatef(0.0f, 0.0f, -1.5f);
    }
    for (i = 0; i < 10; i++) {
      for (j = 0; j < 4; j++) {
        for (k = 0; k < 4; k++) {
          for (l = 0; l < 3; l++) {
            p[(j*4+k)*3+l] = teapotCPData[teapotPatchData[i][j * 4 + k]][l];
            q[(j*4+k)*3+l] =
              teapotCPData[teapotPatchData[i][j * 4 + (3 - k)]][l];
            if (l == 1)
              q[(j*4+k)*3+l] *= -1.0;
            if (i < 6) {
              r[(j*4+k)*3+l] =
                teapotCPData[teapotPatchData[i][j * 4 + (3 - k)]][l];
              if (l == 0)
                r[(j*4+k)*3+l] *= -1.0;
              s[(j*4+k)*3+l] = teapotCPData[teapotPatchData[i][j * 4 + k]][l];
              if (l == 0)
                s[(j*4+k)*3+l] *= -1.0;
              if (l == 1)
                s[(j*4+k)*3+l] *= -1.0;
            }
          }
        }
      }
      gl.glMap2f(GL2.GL_MAP2_TEXTURE_COORD_2, 0, 1, 2, 2, 0, 1, 4, 2, teapotTex, 0);
      gl.glMap2f(GL2.GL_MAP2_VERTEX_3, 0, 1, 3, 4, 0, 1, 12, 4, p, 0);
      gl.glMapGrid2f(grid, 0.0f, 1.0f, grid, 0.0f, 1.0f);
      evaluateTeapotMesh(gl, grid, type, i, !backCompatible);
      gl.glMap2f(GL2.GL_MAP2_VERTEX_3, 0, 1, 3, 4, 0, 1, 12, 4, q, 0);
      evaluateTeapotMesh(gl, grid, type, i, !backCompatible);
      if (i < 6) {
        gl.glMap2f(GL2.GL_MAP2_VERTEX_3, 0, 1, 3, 4, 0, 1, 12, 4, r, 0);
        evaluateTeapotMesh(gl, grid, type, i, !backCompatible);
        gl.glMap2f(GL2.GL_MAP2_VERTEX_3, 0, 1, 3, 4, 0, 1, 12, 4, s, 0);
        evaluateTeapotMesh(gl, grid, type, i, !backCompatible);
      }
    }
    gl.glPopMatrix();
    gl.glPopAttrib();
  }

  private static void evaluateTeapotMesh(final GL2 gl,
                                         final int grid,
                                         final int type,
                                         final int partNum,
                                         final boolean repairSingularities)
  {
    if (repairSingularities && (partNum == 5 || partNum == 3)) {
      // Instead of using evaluators that give bad results at singularities,
      // evaluate by hand
      gl.glPolygonMode(GL.GL_FRONT_AND_BACK, type);
      for (int nv = 0; nv < grid; nv++) {
        if (nv == 0) {
          // Draw a small triangle-fan to fill the hole
          gl.glDisable(GL2.GL_AUTO_NORMAL);
          gl.glNormal3f(0, 0, partNum == 3 ? 1 : -1);
          gl.glBegin(GL.GL_TRIANGLE_FAN);
          {
            gl.glEvalCoord2f(0, 0);
            // Note that we draw in clock-wise order to match the evaluator
            // method
            for (int nu = 0; nu <= grid; nu++)
            {
              gl.glEvalCoord2f(nu / (float)grid, (1f / grid) / grid);
            }
          }
          gl.glEnd();
          gl.glEnable(GL2.GL_AUTO_NORMAL);
        }
        // Draw the rest of the piece as an evaluated quad-strip
        gl.glBegin(GL2.GL_QUAD_STRIP);
        {
          // Note that we draw in clock-wise order to match the evaluator method
          for (int nu = grid; nu >= 0; nu--) {
            gl.glEvalCoord2f(nu / (float)grid, (nv + 1) / (float)grid);
            gl.glEvalCoord2f(nu / (float)grid, Math.max(nv, 1f / grid)
                                                         / grid);
          }
        }
        gl.glEnd();
      }
    } else {
      gl.glEvalMesh2(type, 0, grid, 0, grid);
    }
  }

  //----------------------------------------------------------------------
  // Font implementation
  //

  private static void bitmapCharacterImpl(final GL2 gl, final int font, final char cin) {
    final BitmapFontRec fontinfo = getBitmapFont(font);
    final int c = cin & 0xFFFF;
    if (c < fontinfo.first ||
        c >= fontinfo.first + fontinfo.num_chars)
      return;
    final BitmapCharRec ch = fontinfo.ch[c - fontinfo.first];
    if (ch != null) {
      gl.glBitmap(ch.width, ch.height, ch.xorig, ch.yorig,
                  ch.advance, 0, ch.bitmap, 0);
    }
  }

  private static final BitmapFontRec[] bitmapFonts = new BitmapFontRec[9];
  private static final StrokeFontRec[] strokeFonts = new StrokeFontRec[9];

  private static BitmapFontRec getBitmapFont(final int font) {
    BitmapFontRec rec = bitmapFonts[font];
    if (rec == null) {
      switch (font) {
        case BITMAP_9_BY_15:
          rec = GLUTBitmap9x15.glutBitmap9By15;
          break;
        case BITMAP_8_BY_13:
          rec = GLUTBitmap8x13.glutBitmap8By13;
          break;
        case BITMAP_TIMES_ROMAN_10:
          rec = GLUTBitmapTimesRoman10.glutBitmapTimesRoman10;
          break;
        case BITMAP_TIMES_ROMAN_24:
          rec = GLUTBitmapTimesRoman24.glutBitmapTimesRoman24;
          break;
        case BITMAP_HELVETICA_10:
          rec = GLUTBitmapHelvetica10.glutBitmapHelvetica10;
          break;
        case BITMAP_HELVETICA_12:
          rec = GLUTBitmapHelvetica12.glutBitmapHelvetica12;
          break;
        case BITMAP_HELVETICA_18:
          rec = GLUTBitmapHelvetica18.glutBitmapHelvetica18;
          break;
        default:
          throw new GLException("Unknown bitmap font number " + font);
      }
      bitmapFonts[font] = rec;
    }
    return rec;
  }

  private static StrokeFontRec getStrokeFont(final int font) {
    StrokeFontRec rec = strokeFonts[font];
    if (rec == null) {
      switch (font) {
        case STROKE_ROMAN:
          rec = GLUTStrokeRoman.glutStrokeRoman;
          break;
        case STROKE_MONO_ROMAN:
          rec = GLUTStrokeMonoRoman.glutStrokeMonoRoman;
          break;
        default:
          throw new GLException("Unknown stroke font number " + font);
      }
    }
    return rec;
  }

  private static void beginBitmap(final GL2 gl,
                                  final int[] swapbytes,
                                  final int[] lsbfirst,
                                  final int[] rowlength,
                                  final int[] skiprows,
                                  final int[] skippixels,
                                  final int[] alignment) {
    gl.glGetIntegerv(GL2GL3.GL_UNPACK_SWAP_BYTES, swapbytes, 0);
    gl.glGetIntegerv(GL2GL3.GL_UNPACK_LSB_FIRST, lsbfirst, 0);
    gl.glGetIntegerv(GL2ES2.GL_UNPACK_ROW_LENGTH, rowlength, 0);
    gl.glGetIntegerv(GL2ES2.GL_UNPACK_SKIP_ROWS, skiprows, 0);
    gl.glGetIntegerv(GL2ES2.GL_UNPACK_SKIP_PIXELS, skippixels, 0);
    gl.glGetIntegerv(GL.GL_UNPACK_ALIGNMENT, alignment, 0);
    /* Little endian machines (DEC Alpha for example) could
       benefit from setting GL_UNPACK_LSB_FIRST to GL_TRUE
       instead of GL_FALSE, but this would require changing the
       generated bitmaps too. */
    gl.glPixelStorei(GL2GL3.GL_UNPACK_SWAP_BYTES, GL.GL_FALSE);
    gl.glPixelStorei(GL2GL3.GL_UNPACK_LSB_FIRST, GL.GL_FALSE);
    gl.glPixelStorei(GL2ES2.GL_UNPACK_ROW_LENGTH, 0);
    gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_ROWS, 0);
    gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_PIXELS, 0);
    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
  }

  private static void endBitmap(final GL2 gl,
                                final int[] swapbytes,
                                final int[] lsbfirst,
                                final int[] rowlength,
                                final int[] skiprows,
                                final int[] skippixels,
                                final int[] alignment) {
    /* Restore saved modes. */
    gl.glPixelStorei(GL2GL3.GL_UNPACK_SWAP_BYTES, swapbytes[0]);
    gl.glPixelStorei(GL2GL3.GL_UNPACK_LSB_FIRST, lsbfirst[0]);
    gl.glPixelStorei(GL2ES2.GL_UNPACK_ROW_LENGTH, rowlength[0]);
    gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_ROWS, skiprows[0]);
    gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_PIXELS, skippixels[0]);
    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, alignment[0]);
  }
}
