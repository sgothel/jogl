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
** NOTE:  The Original Code (as defined below) has been licensed to Sun
** Microsystems, Inc. ("Sun") under the SGI Free Software License B
** (Version 1.1), shown above ("SGI License").   Pursuant to Section
** 3.2(3) of the SGI License, Sun is distributing the Covered Code to
** you under an alternative license ("Alternative License").  This
** Alternative License includes all of the provisions of the SGI License
** except that Section 2.2 and 11 are omitted.  Any differences between
** the Alternative License and the SGI License are offered solely by Sun
** and not by SGI.
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
**
** $Date: 2009-03-04 17:23:34 -0800 (Wed, 04 Mar 2009) $ $Revision: 1856 $
** $Header$
*/

/*
 * Copyright (c) 2002-2004 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
 */

package jogamp.opengl.glu;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.ImmModeSink;
import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * GLUquadricImpl.java
 *
 *
 * Created 22-dec-2003 (originally Quadric.java)
 * @author Erik Duijs
 * @author Kenneth Russell, Sven Gothel
 */

public class GLUquadricImpl implements GLUquadric {
  private final boolean useGLSL;
  private int drawStyle;
  private int orientation;
  private boolean textureFlag;
  private int normals;
  private boolean immModeSinkEnabled;
  private boolean immModeSinkImmediate;
  public int normalType;
  public GL gl;
  public ShaderState shaderState;
  public int shaderProgram;

  public static final boolean USE_NORM = true;
  public static final boolean USE_TEXT = false;

  private ImmModeSink immModeSink=null;

  public GLUquadricImpl(final GL gl, final boolean useGLSL, final ShaderState st, final int shaderProgram) {
    this.gl=gl;
    this.useGLSL = useGLSL;
    this.drawStyle = GLU.GLU_FILL;
    this.orientation = GLU.GLU_OUTSIDE;
    this.textureFlag = false;
    this.normals = GLU.GLU_SMOOTH;
    this.normalType = gl.isGLES1()?GL.GL_BYTE:GL.GL_FLOAT;
    this.immModeSinkImmediate=true;
    this.immModeSinkEnabled=!gl.isGL2();
    this.shaderState = st;
    this.shaderProgram = shaderProgram;
    replaceImmModeSink();
  }

  @Override
  public void enableImmModeSink(final boolean val) {
    if(gl.isGL2()) {
        immModeSinkEnabled=val;
    } else {
        immModeSinkEnabled=true;
    }
    if(null==immModeSink && immModeSinkEnabled) {
      replaceImmModeSink();
    }
  }

  @Override
  public boolean isImmModeSinkEnabled() {
    return immModeSinkEnabled;
  }

  @Override
  public void setImmMode(final boolean val) {
    if(immModeSinkEnabled) {
        immModeSinkImmediate=val;
    } else {
        immModeSinkImmediate=true;
    }
  }

  @Override
  public boolean getImmMode() {
    return immModeSinkImmediate;
  }

  @Override
  public ImmModeSink replaceImmModeSink() {
    if(!immModeSinkEnabled) return null;

    final ImmModeSink res = immModeSink;
    if(useGLSL) {
        if(null != shaderState) {
            immModeSink = ImmModeSink.createGLSL (32,
                                                  3, GL.GL_FLOAT,             // vertex
                                                  0, GL.GL_FLOAT,             // color
                                                  USE_NORM?3:0, normalType,   // normal
                                                  USE_TEXT?2:0, GL.GL_FLOAT,  // texCoords
                                                  GL.GL_STATIC_DRAW, shaderState);
        } else {
            immModeSink = ImmModeSink.createGLSL (32,
                                                  3, GL.GL_FLOAT,             // vertex
                                                  0, GL.GL_FLOAT,             // color
                                                  USE_NORM?3:0, normalType,   // normal
                                                  USE_TEXT?2:0, GL.GL_FLOAT,  // texCoords
                                                  GL.GL_STATIC_DRAW, shaderProgram);
        }
    } else {
        immModeSink = ImmModeSink.createFixed(32,
                                              3, GL.GL_FLOAT,             // vertex
                                              0, GL.GL_FLOAT,             // color
                                              USE_NORM?3:0, normalType,   // normal
                                              USE_TEXT?2:0, GL.GL_FLOAT,  // texCoords
                                              GL.GL_STATIC_DRAW);
    }
    return res;
  }

  @Override
  public void resetImmModeSink(final GL gl) {
    if(immModeSinkEnabled) {
        immModeSink.reset(gl);
    }
  }

  /**
   * specifies the draw style for quadrics.
   *
   * The legal values are as follows:
   *
   * GLU.FILL:       Quadrics are rendered with polygon primitives. The polygons
   *                 are drawn in a counterclockwise fashion with respect to
   *                 their normals (as defined with glu.quadricOrientation).
   *
   * GLU.LINE:       Quadrics are rendered as a set of lines.
   *
   * GLU.SILHOUETTE: Quadrics are rendered as a set of lines, except that edges
   *            separating coplanar faces will not be drawn.
   *
   * GLU.POINT:       Quadrics are rendered as a set of points.
   *
   * @param drawStyle The drawStyle to set
   */
  public void setDrawStyle(final int drawStyle) {
    this.drawStyle = drawStyle;
  }

  /**
   * specifies what kind    of normals are desired for quadrics.
   * The legal values    are as follows:
   *
   * GLU.NONE:     No normals are generated.
   *
   * GLU.FLAT:     One normal is generated for every facet of a quadric.
   *
   * GLU.SMOOTH:   One normal is generated for every vertex of a quadric.  This
   *               is the default.
   *
   * @param normals The normals to set
   */
  public void setNormals(final int normals) {
    this.normals = normals;
  }

  /**
   * specifies what kind of orientation is desired for.
   * The orientation    values are as follows:
   *
   * GLU.OUTSIDE:  Quadrics are drawn with normals pointing outward.
   *
   * GLU.INSIDE:   Normals point inward. The default is GLU.OUTSIDE.
   *
   * Note that the interpretation of outward and inward depends on the quadric
   * being drawn.
   *
   * @param orientation The orientation to set
   */
  public void setOrientation(final int orientation) {
    this.orientation = orientation;
  }

  /**
   * specifies if texture coordinates should be generated for
   * quadrics rendered with qobj. If the value of textureCoords is true,
   * then texture coordinates are generated, and if textureCoords is false,
   * they are not.. The default is false.
   *
   * The manner in which texture coordinates are generated depends upon the
   * specific quadric rendered.
   *
   * @param textureFlag The textureFlag to set
   */
  public void setTextureFlag(final boolean textureFlag) {
    this.textureFlag = textureFlag;
  }

  /**
   * Returns the drawStyle.
   * @return int
   */
  public int getDrawStyle() {
    return drawStyle;
  }

  /**
   * Returns the normals.
   * @return int
   */
  public int getNormals() {
    return normals;
  }

  /**
   * Returns the orientation.
   * @return int
   */
  public int getOrientation() {
    return orientation;
  }

  /**
   * Returns the textureFlag.
   * @return boolean
   */
  public boolean getTextureFlag() {
    return textureFlag;
  }


  /**
   * draws a cylinder oriented along the z axis. The base of the
   * cylinder is placed at z = 0, and the top at z=height. Like a sphere, a
   * cylinder is subdivided around the z axis into slices, and along the z axis
   * into stacks.
   *
   * Note that if topRadius is set to zero, then this routine will generate a
   * cone.
   *
   * If the orientation is set to GLU.OUTSIDE (with glu.quadricOrientation), then
   * any generated normals point away from the z axis. Otherwise, they point
   * toward the z axis.
   *
   * If texturing is turned on (with glu.quadricTexture), then texture
   * coordinates are generated so that t ranges linearly from 0.0 at z = 0 to
   * 1.0 at z = height, and s ranges from 0.0 at the +y axis, to 0.25 at the +x
   * axis, to 0.5 at the -y axis, to 0.75 at the -x axis, and back to 1.0 at the
   * +y axis.
   *
   * @param baseRadius  Specifies the radius of the cylinder at z = 0.
   * @param topRadius   Specifies the radius of the cylinder at z = height.
   * @param height      Specifies the height of the cylinder.
   * @param slices      Specifies the number of subdivisions around the z axis.
   * @param stacks      Specifies the number of subdivisions along the z axis.
   */
  public void drawCylinder(final GL gl, final float baseRadius, final float topRadius, final float height, final int slices, final int stacks) {

    float da, r, dr, dz;
    float x, y, z, nz, nsign;
    int i, j;

    if (orientation == GLU.GLU_INSIDE) {
      nsign = -1.0f;
    } else {
      nsign = 1.0f;
    }

    da = PI_2 / slices;
    dr = (topRadius - baseRadius) / stacks;
    dz = height / stacks;
    nz = (baseRadius - topRadius) / height;
    // Z component of normal vectors

    if (drawStyle == GLU.GLU_POINT) {
      glBegin(gl, GL.GL_POINTS);
      for (i = 0; i < slices; i++) {
        x = cos((i * da));
        y = sin((i * da));
        normal3f(gl, x * nsign, y * nsign, nz * nsign);

        z = 0.0f;
        r = baseRadius;
        for (j = 0; j <= stacks; j++) {
          glVertex3f(gl, (x * r), (y * r), z);
          z += dz;
          r += dr;
        }
      }
      glEnd(gl);
    } else if (drawStyle == GLU.GLU_LINE || drawStyle == GLU.GLU_SILHOUETTE) {
      // Draw rings
      if (drawStyle == GLU.GLU_LINE) {
        z = 0.0f;
        r = baseRadius;
        for (j = 0; j <= stacks; j++) {
          glBegin(gl, GL.GL_LINE_LOOP);
          for (i = 0; i < slices; i++) {
            x = cos((i * da));
            y = sin((i * da));
            normal3f(gl, x * nsign, y * nsign, nz * nsign);
            glVertex3f(gl, (x * r), (y * r), z);
          }
          glEnd(gl);
          z += dz;
          r += dr;
        }
      } else {
        // draw one ring at each end
        if (baseRadius != 0.0) {
          glBegin(gl, GL.GL_LINE_LOOP);
          for (i = 0; i < slices; i++) {
            x = cos((i * da));
            y = sin((i * da));
            normal3f(gl, x * nsign, y * nsign, nz * nsign);
            glVertex3f(gl, (x * baseRadius), (y * baseRadius), 0.0f);
          }
          glEnd(gl);
          glBegin(gl, GL.GL_LINE_LOOP);
          for (i = 0; i < slices; i++) {
            x = cos((i * da));
            y = sin((i * da));
            normal3f(gl, x * nsign, y * nsign, nz * nsign);
            glVertex3f(gl, (x * topRadius), (y * topRadius), height);
          }
          glEnd(gl);
        }
      }
      // draw length lines
      glBegin(gl, GL.GL_LINES);
      for (i = 0; i < slices; i++) {
        x = cos((i * da));
        y = sin((i * da));
        normal3f(gl, x * nsign, y * nsign, nz * nsign);
        glVertex3f(gl, (x * baseRadius), (y * baseRadius), 0.0f);
        glVertex3f(gl, (x * topRadius), (y * topRadius), (height));
      }
      glEnd(gl);
    } else if (drawStyle == GLU.GLU_FILL) {
      final float ds = 1.0f / slices;
      final float dt = 1.0f / stacks;
      float t = 0.0f;
      z = 0.0f;
      r = baseRadius;
      for (j = 0; j < stacks; j++) {
        float s = 0.0f;
        glBegin(gl, ImmModeSink.GL_QUAD_STRIP);
        for (i = 0; i <= slices; i++) {
          if (i == slices) {
            x = sin(0.0f);
            y = cos(0.0f);
          } else {
            x = sin((i * da));
            y = cos((i * da));
          }
          // if (nsign == 1.0f) {
            normal3f(gl, (x * nsign), (y * nsign), (nz * nsign));
            TXTR_COORD(gl, s, t);
            glVertex3f(gl, (x * r), (y * r), z);
            normal3f(gl, (x * nsign), (y * nsign), (nz * nsign));
            TXTR_COORD(gl, s, t + dt);
            glVertex3f(gl, (x * (r + dr)), (y * (r + dr)), (z + dz));
          /* } else {
            normal3f(gl, x * nsign, y * nsign, nz * nsign);
            TXTR_COORD(gl, s, t);
            glVertex3f(gl, (x * r), (y * r), z);
            normal3f(gl, x * nsign, y * nsign, nz * nsign);
            TXTR_COORD(gl, s, t + dt);
            glVertex3f(gl, (x * (r + dr)), (y * (r + dr)), (z + dz));
          } */
          s += ds;
        } // for slices
        glEnd(gl);
        r += dr;
        t += dt;
        z += dz;
      } // for stacks
    }
  }

  /**
   * renders a disk on the z = 0  plane.  The disk has a radius of
   * outerRadius, and contains a concentric circular hole with a radius of
   * innerRadius. If innerRadius is 0, then no hole is generated. The disk is
   * subdivided around the z axis into slices (like pizza slices), and also
   * about the z axis into rings (as specified by slices and loops,
   * respectively).
   *
   * With respect to orientation, the +z side of the disk is considered to be
   * "outside" (see glu.quadricOrientation).  This means that if the orientation
   * is set to GLU.OUTSIDE, then any normals generated point along the +z axis.
   * Otherwise, they point along the -z axis.
   *
   * If texturing is turned on (with glu.quadricTexture), texture coordinates are
   * generated linearly such that where r=outerRadius, the value at (r, 0, 0) is
   * (1, 0.5), at (0, r, 0) it is (0.5, 1), at (-r, 0, 0) it is (0, 0.5), and at
   * (0, -r, 0) it is (0.5, 0).
   */
  public void drawDisk(final GL gl, final float innerRadius, final float outerRadius, final int slices, final int loops)
  {
    float da, dr;

    /* Normal vectors */
    if (normals != GLU.GLU_NONE) {
      if (orientation == GLU.GLU_OUTSIDE) {
        glNormal3f(gl, 0.0f, 0.0f, +1.0f);
      }
      else {
        glNormal3f(gl, 0.0f, 0.0f, -1.0f);
      }
    }

    da = PI_2 / slices;
    dr = (outerRadius - innerRadius) /  loops;

    switch (drawStyle) {
    case GLU.GLU_FILL:
      {
        /* texture of a gluDisk is a cut out of the texture unit square
         * x, y in [-outerRadius, +outerRadius]; s, t in [0, 1]
         * (linear mapping)
         */
        final float dtc = 2.0f * outerRadius;
        float sa, ca;
        float r1 = innerRadius;
        int l;
        for (l = 0; l < loops; l++) {
          final float r2 = r1 + dr;
          if (orientation == GLU.GLU_OUTSIDE) {
            int s;
            glBegin(gl, ImmModeSink.GL_QUAD_STRIP);
            for (s = 0; s <= slices; s++) {
              float a;
              if (s == slices)
                a = 0.0f;
              else
                a = s * da;
              sa = sin(a);
              ca = cos(a);
              TXTR_COORD(gl, 0.5f + sa * r2 / dtc, 0.5f + ca * r2 / dtc);
              glVertex2f(gl, r2 * sa, r2 * ca);
              TXTR_COORD(gl, 0.5f + sa * r1 / dtc, 0.5f + ca * r1 / dtc);
              glVertex2f(gl, r1 * sa, r1 * ca);
            }
            glEnd(gl);
          }
          else {
            int s;
            glBegin(gl, ImmModeSink.GL_QUAD_STRIP);
            for (s = slices; s >= 0; s--) {
              float a;
              if (s == slices)
                a = 0.0f;
              else
                a = s * da;
              sa = sin(a);
              ca = cos(a);
              TXTR_COORD(gl, 0.5f - sa * r2 / dtc, 0.5f + ca * r2 / dtc);
              glVertex2f(gl, r2 * sa, r2 * ca);
              TXTR_COORD(gl, 0.5f - sa * r1 / dtc, 0.5f + ca * r1 / dtc);
              glVertex2f(gl, r1 * sa, r1 * ca);
            }
            glEnd(gl);
          }
          r1 = r2;
        }
        break;
      }
    case GLU.GLU_LINE:
      {
        int l, s;
        /* draw loops */
        for (l = 0; l <= loops; l++) {
          final float r = innerRadius + l * dr;
          glBegin(gl, GL.GL_LINE_LOOP);
          for (s = 0; s < slices; s++) {
            final float a = s * da;
            glVertex2f(gl, r * sin(a), r * cos(a));
          }
          glEnd(gl);
        }
        /* draw spokes */
        for (s = 0; s < slices; s++) {
          final float a = s * da;
          final float x = sin(a);
          final float y = cos(a);
          glBegin(gl, GL.GL_LINE_STRIP);
          for (l = 0; l <= loops; l++) {
            final float r = innerRadius + l * dr;
            glVertex2f(gl, r * x, r * y);
          }
          glEnd(gl);
        }
        break;
      }
    case GLU.GLU_POINT:
      {
        int s;
        glBegin(gl, GL.GL_POINTS);
        for (s = 0; s < slices; s++) {
          final float a = s * da;
          final float x = sin(a);
          final float y = cos(a);
          int l;
          for (l = 0; l <= loops; l++) {
            final float r = innerRadius * l * dr;
            glVertex2f(gl, r * x, r * y);
          }
        }
        glEnd(gl);
        break;
      }
    case GLU.GLU_SILHOUETTE:
      {
        if (innerRadius != 0.0) {
          float a;
          glBegin(gl, GL.GL_LINE_LOOP);
          for (a = 0.0f; a < PI_2; a += da) {
            final float x = innerRadius * sin(a);
            final float y = innerRadius * cos(a);
            glVertex2f(gl, x, y);
          }
          glEnd(gl);
        }
        {
          float a;
          glBegin(gl, GL.GL_LINE_LOOP);
          for (a = 0; a < PI_2; a += da) {
            final float x = outerRadius * sin(a);
            final float y = outerRadius * cos(a);
            glVertex2f(gl, x, y);
          }
          glEnd(gl);
        }
        break;
      }
    default:
      return;
    }
  }

  /**
   * renders a partial disk on the z=0 plane. A partial disk is similar to a
   * full disk, except that only the subset of the disk from startAngle
   * through startAngle + sweepAngle is included (where 0 degrees is along
   * the +y axis, 90 degrees along the +x axis, 180 along the -y axis, and
   * 270 along the -x axis).
   *
   * The partial disk has a radius of outerRadius, and contains a concentric
   * circular hole with a radius of innerRadius. If innerRadius is zero, then
   * no hole is generated. The partial disk is subdivided around the z axis
   * into slices (like pizza slices), and also about the z axis into rings
   * (as specified by slices and loops, respectively).
   *
   * With respect to orientation, the +z side of the partial disk is
   * considered to be outside (see gluQuadricOrientation). This means that if
   * the orientation is set to GLU.GLU_OUTSIDE, then any normals generated point
   * along the +z axis. Otherwise, they point along the -z axis.
   *
   * If texturing is turned on (with gluQuadricTexture), texture coordinates
   * are generated linearly such that where r=outerRadius, the value at (r, 0, 0)
   * is (1, 0.5), at (0, r, 0) it is (0.5, 1), at (-r, 0, 0) it is (0, 0.5),
   * and at (0, -r, 0) it is (0.5, 0).
   */
  public void drawPartialDisk(final GL gl,
                              final float innerRadius,
                              final float outerRadius,
                              int slices,
                              final int loops,
                              float startAngle,
                              float sweepAngle) {
    int i, j;
    final float[] sinCache = new float[CACHE_SIZE];
    final float[] cosCache = new float[CACHE_SIZE];
    float angle;
    float sintemp, costemp;
    float deltaRadius;
    float radiusLow, radiusHigh;
    float texLow = 0, texHigh = 0;
    float angleOffset;
    int slices2;
    int finish;

    if (slices >= CACHE_SIZE)
      slices = CACHE_SIZE - 1;
    if (slices < 2
        || loops < 1
        || outerRadius <= 0.0f
        || innerRadius < 0.0f
        || innerRadius > outerRadius) {
      //gluQuadricError(qobj, GLU.GLU_INVALID_VALUE);
      System.err.println("PartialDisk: GLU_INVALID_VALUE");
      return;
    }

    if (sweepAngle < -360.0f)
      sweepAngle = 360.0f;
    if (sweepAngle > 360.0f)
      sweepAngle = 360.0f;
    if (sweepAngle < 0) {
      startAngle += sweepAngle;
      sweepAngle = -sweepAngle;
    }

    if (sweepAngle == 360.0f) {
      slices2 = slices;
    } else {
      slices2 = slices + 1;
    }

    /* Compute length (needed for normal calculations) */
    deltaRadius = outerRadius - innerRadius;

    /* Cache is the vertex locations cache */

    angleOffset = startAngle / 180.0f * PI;
    for (i = 0; i <= slices; i++) {
      angle = angleOffset + ((PI * sweepAngle) / 180.0f) * i / slices;
      sinCache[i] = sin(angle);
      cosCache[i] = cos(angle);
    }

    if (sweepAngle == 360.0f) {
      sinCache[slices] = sinCache[0];
      cosCache[slices] = cosCache[0];
    }

    switch (normals) {
    case GLU.GLU_FLAT :
    case GLU.GLU_SMOOTH :
      if (orientation == GLU.GLU_OUTSIDE) {
        glNormal3f(gl, 0.0f, 0.0f, 1.0f);
      } else {
        glNormal3f(gl, 0.0f, 0.0f, -1.0f);
      }
      break;
    default :
    case GLU.GLU_NONE :
      break;
    }

    switch (drawStyle) {
    case GLU.GLU_FILL :
      if (innerRadius == .0f) {
        finish = loops - 1;
        /* Triangle strip for inner polygons */
        glBegin(gl, GL.GL_TRIANGLE_FAN);
        if (textureFlag) {
          glTexCoord2f(gl, 0.5f, 0.5f);
        }
        glVertex3f(gl, 0.0f, 0.0f, 0.0f);
        radiusLow = outerRadius - deltaRadius * ((float) (loops - 1) / loops);
        if (textureFlag) {
          texLow = radiusLow / outerRadius / 2;
        }

        if (orientation == GLU.GLU_OUTSIDE) {
          for (i = slices; i >= 0; i--) {
            if (textureFlag) {
              glTexCoord2f(gl, texLow * sinCache[i] + 0.5f,
                              texLow * cosCache[i] + 0.5f);
            }
            glVertex3f(gl, radiusLow * sinCache[i], radiusLow * cosCache[i], 0.0f);
          }
        } else {
          for (i = 0; i <= slices; i++) {
            if (textureFlag) {
              glTexCoord2f(gl, texLow * sinCache[i] + 0.5f,
                              texLow * cosCache[i] + 0.5f);
            }
            glVertex3f(gl, radiusLow * sinCache[i], radiusLow * cosCache[i], 0.0f);
          }
        }
        glEnd(gl);
      } else {
        finish = loops;
      }
      for (j = 0; j < finish; j++) {
        radiusLow = outerRadius - deltaRadius * ((float) j / loops);
        radiusHigh = outerRadius - deltaRadius * ((float) (j + 1) / loops);
        if (textureFlag) {
          texLow = radiusLow / outerRadius / 2;
          texHigh = radiusHigh / outerRadius / 2;
        }

        glBegin(gl, ImmModeSink.GL_QUAD_STRIP);
        for (i = 0; i <= slices; i++) {
          if (orientation == GLU.GLU_OUTSIDE) {
            if (textureFlag) {
              glTexCoord2f(gl, texLow * sinCache[i] + 0.5f,
                              texLow * cosCache[i] + 0.5f);
            }
            glVertex3f(gl, radiusLow * sinCache[i], radiusLow * cosCache[i], 0.0f);

            if (textureFlag) {
              glTexCoord2f(gl, texHigh * sinCache[i] + 0.5f,
                              texHigh * cosCache[i] + 0.5f);
            }
            glVertex3f(gl, radiusHigh * sinCache[i],
                          radiusHigh * cosCache[i],
                          0.0f);
          } else {
            if (textureFlag) {
              glTexCoord2f(gl, texHigh * sinCache[i] + 0.5f,
                              texHigh * cosCache[i] + 0.5f);
            }
            glVertex3f(gl, radiusHigh * sinCache[i],
                          radiusHigh * cosCache[i],
                          0.0f);

            if (textureFlag) {
              glTexCoord2f(gl, texLow * sinCache[i] + 0.5f,
                              texLow * cosCache[i] + 0.5f);
            }
            glVertex3f(gl, radiusLow * sinCache[i], radiusLow * cosCache[i], 0.0f);
          }
        }
        glEnd(gl);
      }
      break;
    case GLU.GLU_POINT :
      glBegin(gl, GL.GL_POINTS);
      for (i = 0; i < slices2; i++) {
        sintemp = sinCache[i];
        costemp = cosCache[i];
        for (j = 0; j <= loops; j++) {
          radiusLow = outerRadius - deltaRadius * ((float) j / loops);

          if (textureFlag) {
            texLow = radiusLow / outerRadius / 2;

            glTexCoord2f(gl, texLow * sinCache[i] + 0.5f,
                            texLow * cosCache[i] + 0.5f);
          }
          glVertex3f(gl, radiusLow * sintemp, radiusLow * costemp, 0.0f);
        }
      }
      glEnd(gl);
      break;
    case GLU.GLU_LINE :
      if (innerRadius == outerRadius) {
        glBegin(gl, GL.GL_LINE_STRIP);

        for (i = 0; i <= slices; i++) {
          if (textureFlag) {
            glTexCoord2f(gl, sinCache[i] / 2 + 0.5f, cosCache[i] / 2 + 0.5f);
          }
          glVertex3f(gl, innerRadius * sinCache[i], innerRadius * cosCache[i], 0.0f);
        }
        glEnd(gl);
        break;
      }
      for (j = 0; j <= loops; j++) {
        radiusLow = outerRadius - deltaRadius * ((float) j / loops);
        if (textureFlag) {
          texLow = radiusLow / outerRadius / 2;
        }

        glBegin(gl, GL.GL_LINE_STRIP);
        for (i = 0; i <= slices; i++) {
          if (textureFlag) {
            glTexCoord2f(gl, texLow * sinCache[i] + 0.5f,
                            texLow * cosCache[i] + 0.5f);
          }
          glVertex3f(gl, radiusLow * sinCache[i], radiusLow * cosCache[i], 0.0f);
        }
        glEnd(gl);
      }
      for (i = 0; i < slices2; i++) {
        sintemp = sinCache[i];
        costemp = cosCache[i];
        glBegin(gl, GL.GL_LINE_STRIP);
        for (j = 0; j <= loops; j++) {
          radiusLow = outerRadius - deltaRadius * ((float) j / loops);
          if (textureFlag) {
            texLow = radiusLow / outerRadius / 2;
          }

          if (textureFlag) {
            glTexCoord2f(gl, texLow * sinCache[i] + 0.5f,
                            texLow * cosCache[i] + 0.5f);
          }
          glVertex3f(gl, radiusLow * sintemp, radiusLow * costemp, 0.0f);
        }
        glEnd(gl);
      }
      break;
    case GLU.GLU_SILHOUETTE :
      if (sweepAngle < 360.0f) {
        for (i = 0; i <= slices; i += slices) {
          sintemp = sinCache[i];
          costemp = cosCache[i];
          glBegin(gl, GL.GL_LINE_STRIP);
          for (j = 0; j <= loops; j++) {
            radiusLow = outerRadius - deltaRadius * ((float) j / loops);

            if (textureFlag) {
              texLow = radiusLow / outerRadius / 2;
              glTexCoord2f(gl, texLow * sinCache[i] + 0.5f,
                              texLow * cosCache[i] + 0.5f);
            }
            glVertex3f(gl, radiusLow * sintemp, radiusLow * costemp, 0.0f);
          }
          glEnd(gl);
        }
      }
      for (j = 0; j <= loops; j += loops) {
        radiusLow = outerRadius - deltaRadius * ((float) j / loops);
        if (textureFlag) {
          texLow = radiusLow / outerRadius / 2;
        }

        glBegin(gl, GL.GL_LINE_STRIP);
        for (i = 0; i <= slices; i++) {
          if (textureFlag) {
            glTexCoord2f(gl, texLow * sinCache[i] + 0.5f,
                            texLow * cosCache[i] + 0.5f);
          }
          glVertex3f(gl, radiusLow * sinCache[i], radiusLow * cosCache[i], 0.0f);
        }
        glEnd(gl);
        if (innerRadius == outerRadius)
          break;
      }
      break;
    default :
      break;
    }
  }

  /**
   * draws a sphere of the given    radius centered    around the origin.
   * The sphere is subdivided around the z axis into slices and along the z axis
   * into stacks (similar to lines of longitude and latitude).
   *
   * If the orientation is set to GLU.OUTSIDE (with glu.quadricOrientation), then
   * any normals generated point away from the center of the sphere. Otherwise,
   * they point toward the center of the sphere.

   * If texturing is turned on (with glu.quadricTexture), then texture
   * coordinates are generated so that t ranges from 0.0 at z=-radius to 1.0 at
   * z=radius (t increases linearly along longitudinal lines), and s ranges from
   * 0.0 at the +y axis, to 0.25 at the +x axis, to 0.5 at the -y axis, to 0.75
   * at the -x axis, and back to 1.0 at the +y axis.
   */
  public void drawSphere(final GL gl, final float radius, final int slices, final int stacks) {
    // TODO

    float rho, drho, theta, dtheta;
    float x, y, z;
    float s, t, ds, dt;
    int i, j, imin, imax;
    boolean normals;
    float nsign;

    normals = (this.normals != GLU.GLU_NONE);

    if (orientation == GLU.GLU_INSIDE) {
      nsign = -1.0f;
    } else {
      nsign = 1.0f;
    }

    drho = PI / stacks;
    dtheta = PI_2 / slices;

    if (drawStyle == GLU.GLU_FILL) {
      if (!textureFlag) {
        // draw +Z end as a triangle fan
        glBegin(gl, GL.GL_TRIANGLE_FAN);
        glNormal3f(gl, 0.0f, 0.0f, 1.0f);
        glVertex3f(gl, 0.0f, 0.0f, nsign * radius);
        for (j = 0; j <= slices; j++) {
          theta = (j == slices) ? 0.0f : j * dtheta;
          x = -sin(theta) * sin(drho);
          y = cos(theta) * sin(drho);
          z = nsign * cos(drho);
          if (normals) {
            glNormal3f(gl, x * nsign, y * nsign, z * nsign);
          }
          glVertex3f(gl, x * radius, y * radius, z * radius);
        }
        glEnd(gl);
      }

      ds = 1.0f / slices;
      dt = 1.0f / stacks;
      t = 1.0f; // because loop now runs from 0
      if (textureFlag) {
        imin = 0;
        imax = stacks;
      } else {
        imin = 1;
        imax = stacks - 1;
      }

      // draw intermediate stacks as quad strips
      for (i = imin; i < imax; i++) {
        rho = i * drho;
        glBegin(gl, ImmModeSink.GL_QUAD_STRIP);
        s = 0.0f;
        for (j = 0; j <= slices; j++) {
          theta = (j == slices) ? 0.0f : j * dtheta;
          x = -sin(theta) * sin(rho);
          y = cos(theta) * sin(rho);
          z = nsign * cos(rho);
          if (normals) {
            glNormal3f(gl, x * nsign, y * nsign, z * nsign);
          }
          TXTR_COORD(gl, s, t);
          glVertex3f(gl, x * radius, y * radius, z * radius);
          x = -sin(theta) * sin(rho + drho);
          y = cos(theta) * sin(rho + drho);
          z = nsign * cos(rho + drho);
          if (normals) {
            glNormal3f(gl, x * nsign, y * nsign, z * nsign);
          }
          TXTR_COORD(gl, s, t - dt);
          s += ds;
          glVertex3f(gl, x * radius, y * radius, z * radius);
        }
        glEnd(gl);
        t -= dt;
      }

      if (!textureFlag) {
        // draw -Z end as a triangle fan
        glBegin(gl, GL.GL_TRIANGLE_FAN);
        glNormal3f(gl, 0.0f, 0.0f, -1.0f);
        glVertex3f(gl, 0.0f, 0.0f, -radius * nsign);
        rho = PI - drho;
        s = 1.0f;
        for (j = slices; j >= 0; j--) {
          theta = (j == slices) ? 0.0f : j * dtheta;
          x = -sin(theta) * sin(rho);
          y = cos(theta) * sin(rho);
          z = nsign * cos(rho);
          if (normals)
            glNormal3f(gl, x * nsign, y * nsign, z * nsign);
          s -= ds;
          glVertex3f(gl, x * radius, y * radius, z * radius);
        }
        glEnd(gl);
      }
    } else if (
               drawStyle == GLU.GLU_LINE
               || drawStyle == GLU.GLU_SILHOUETTE) {
      // draw stack lines
      for (i = 1;
           i < stacks;
           i++) { // stack line at i==stacks-1 was missing here
        rho = i * drho;
        glBegin(gl, GL.GL_LINE_LOOP);
        for (j = 0; j < slices; j++) {
          theta = j * dtheta;
          x = cos(theta) * sin(rho);
          y = sin(theta) * sin(rho);
          z = cos(rho);
          if (normals)
            glNormal3f(gl, x * nsign, y * nsign, z * nsign);
          glVertex3f(gl, x * radius, y * radius, z * radius);
        }
        glEnd(gl);
      }
      // draw slice lines
      for (j = 0; j < slices; j++) {
        theta = j * dtheta;
        glBegin(gl, GL.GL_LINE_STRIP);
        for (i = 0; i <= stacks; i++) {
          rho = i * drho;
          x = cos(theta) * sin(rho);
          y = sin(theta) * sin(rho);
          z = cos(rho);
          if (normals)
            glNormal3f(gl, x * nsign, y * nsign, z * nsign);
          glVertex3f(gl, x * radius, y * radius, z * radius);
        }
        glEnd(gl);
      }
    } else if (drawStyle == GLU.GLU_POINT) {
      // top and bottom-most points
      glBegin(gl, GL.GL_POINTS);
      if (normals)
        glNormal3f(gl, 0.0f, 0.0f, nsign);
      glVertex3f(gl, 0.0f, 0.0f, radius);
      if (normals)
        glNormal3f(gl, 0.0f, 0.0f, -nsign);
      glVertex3f(gl, 0.0f, 0.0f, -radius);

      // loop over stacks
      for (i = 1; i < stacks - 1; i++) {
        rho = i * drho;
        for (j = 0; j < slices; j++) {
          theta = j * dtheta;
          x = cos(theta) * sin(rho);
          y = sin(theta) * sin(rho);
          z = cos(rho);
          if (normals)
            glNormal3f(gl, x * nsign, y * nsign, z * nsign);
          glVertex3f(gl, x * radius, y * radius, z * radius);
        }
      }
      glEnd(gl);
    }
  }


  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static final float PI = FloatUtil.PI;
  private static final float PI_2 = 2f * PI;
  private static final int CACHE_SIZE = 240;

  private final void glBegin(final GL gl, final int mode) {
      if(immModeSinkEnabled) {
          immModeSink.glBegin(mode);
      } else {
          gl.getGL2().glBegin(mode);
      }
  }

  private final void glEnd(final GL gl) {
      if(immModeSinkEnabled) {
          immModeSink.glEnd(gl, immModeSinkImmediate);
      } else {
          gl.getGL2().glEnd();
      }
  }

  private final void glVertex2f(final GL gl, final float x, final float y) {
      if(immModeSinkEnabled) {
          immModeSink.glVertex2f(x, y);
      } else {
          gl.getGL2().glVertex2f(x, y);
      }
  }

  private final void glVertex3f(final GL gl, final float x, final float y, final float z) {
      if(immModeSinkEnabled) {
          immModeSink.glVertex3f(x, y, z);
      } else {
          gl.getGL2().glVertex3f(x, y, z);
      }
  }

  private final void glNormal3f_s(final GL gl, final float x, final float y, final float z) {
      final short a=(short)(x*0xFFFF);
      final short b=(short)(y*0xFFFF);
      final short c=(short)(z*0xFFFF);
      if(immModeSinkEnabled) {
          immModeSink.glNormal3s(a, b, c);
      } else {
          gl.getGL2().glNormal3s(a, b, c);
      }
  }

  private final void glNormal3f_b(final GL gl, final float x, final float y, final float z) {
      final byte a=(byte)(x*0xFF);
      final byte b=(byte)(y*0xFF);
      final byte c=(byte)(z*0xFF);
      if(immModeSinkEnabled) {
          immModeSink.glNormal3b(a, b, c);
      } else {
          gl.getGL2().glNormal3b(a, b, c);
      }
  }

  private final void glNormal3f(final GL gl, final float x, final float y, final float z) {
    switch(normalType) {
        case GL.GL_FLOAT:
            if(immModeSinkEnabled) {
                immModeSink.glNormal3f(x,y,z);
            } else {
                gl.getGL2().glNormal3f(x,y,z);
            }
            break;
        case GL.GL_SHORT:
            glNormal3f_s(gl, x, y, z);
            break;
        case GL.GL_BYTE:
            glNormal3f_b(gl, x, y, z);
            break;
    }
  }

  private final void glTexCoord2f(final GL gl, final float x, final float y) {
      if(immModeSinkEnabled) {
          immModeSink.glTexCoord2f(x, y);
      } else {
          gl.getGL2().glTexCoord2f(x, y);
      }
  }

  /**
   * Call glNormal3f after scaling normal to unit length.
   *
   * @param x
   * @param y
   * @param z
   */
  private void normal3f(final GL gl, float x, float y, float z) {
    float mag;

    mag = (float)Math.sqrt(x * x + y * y + z * z);
    if (mag > 0.00001F) {
      x /= mag;
      y /= mag;
      z /= mag;
    }
    glNormal3f(gl, x, y, z);
  }

  private final void TXTR_COORD(final GL gl, final float x, final float y) {
    if (textureFlag) glTexCoord2f(gl, x,y);
  }

  private float sin(final float r) {
    return (float)Math.sin(r);
  }

  private float cos(final float r) {
    return (float)Math.cos(r);
  }
}
