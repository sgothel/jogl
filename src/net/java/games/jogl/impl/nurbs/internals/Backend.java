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

public class Backend {
  private BasicCurveEvaluator curveEvaluator;
  private BasicSurfaceEvaluator surfaceEvaluator;

  public Backend( BasicCurveEvaluator c, BasicSurfaceEvaluator e ) {
    this.c = c;
    this.e = e;
  }

  /* surface backend routines */

  /**
   * bgnsurf - preamble to surface definition and evaluations
   */
  public void bgnsurf( boolean wiretris, boolean wirequads, long nuid ) {
    wireframetris = wiretris;
    wireframequads = wirequads;

    /*in the spec, GLU_DISPLAY_MODE is either
     * GLU_FILL
     * GLU_OUTLINE_POLY
     * GLU_OUTLINE_PATCH.
     *In fact, GLU_FLL is has the same effect as
     * set GL_FRONT_AND_BACK to be GL_FILL
     * and GLU_OUTLINE_POLY is the same as set 
     *     GL_FRONT_AND_BACK to be GL_LINE
     *It is more efficient to do this once at the beginning of
     *each surface than to do it for each primitive.
     *   The internal has more options: outline_triangle and outline_quad
     *can be seperated. But since this is not in spec, and more importantly,
     *this is not so useful, so we don't need to keep this option.
     */

    surfaceEvaluator.bgnmap2f( nuid );

    if(wiretris)
      surfaceEvaluator.polymode(N_MESHLINE);
    else
      surfaceEvaluator.polymode(N_MESHFILL);
  }

  public void patch( float ulo, float uhi, float vlo, float vhi ) {
    surfaceEvaluator.domain2f( ulo, uhi, vlo, vhi );
  }

  /**
   * surfpts - pass a desription of a surface map
   */
  public void surfpts(long type,    /* geometry, color, texture, normal      */
                      float[] pts,  /* control points                        */
                      long ustride, /* distance to next point in u direction */
                      long vstride, /* distance to next point in v direction */
                      int uorder,   /* u parametric order                    */
                      int vorder,   /* v parametric order                    */
                      float ulo,    /* u lower bound                         */
                      float uhi,    /* u upper bound                         */
                      float vlo,    /* v lower bound                         */
                      float vhi     /* v upper bound                         */ ) {
    surfaceEvaluator.map2f( type,ulo,uhi,ustride,uorder,vlo,vhi,vstride,vorder,pts );
    surfaceEvaluator.enable( type );
  }
  public void surfbbox( long type, float[] from, float[] to ) {
    surfaceEvaluator.range2f( type, from, to );
  }
  /**
   * surfgrid - define a lattice of points with origin and offset
   */
  public void surfgrid( float u0, float u1, long nu, float v0, float v1, long nv ) {
    surfaceEvaluator.mapgrid2f( nu, u0, u1, nv, v0, v1 );
  }
  /**
   * surfmesh - evaluate a mesh of points on lattice
   */
  public void surfmesh( long u, long v, long n, long m ) {
    if( wireframequads ) {
      long v0,  v1;
      long u0f = u, u1f = u+n;
      long v0f = v, v1f = v+m;
      long parity = (u & 1);

      for( v0 = v0f, v1 = v0f++ ; v0<v1f; v0 = v1, v1++ ) {
        surfaceEvaluator.bgnline();
        for( long u = u0f; u<=u1f; u++ ) {
          if( parity ) {
            surfaceEvaluator.evalpoint2i( u, v0 );
            surfaceEvaluator.evalpoint2i( u, v1 );
          } else {
            surfaceEvaluator.evalpoint2i( u, v1 );
            surfaceEvaluator.evalpoint2i( u, v0 );
          }
          parity = 1 - parity;
        }
        surfaceEvaluator.endline();
      }
    } else {
      surfaceEvaluator.mapmesh2f( N_MESHFILL, u, u+n, v, v+m );
    }
  }
  /**
   * bgntmesh - preamble to a triangle mesh
   */
  public void bgntmesh() {
    meshindex = 0;	/* I think these need to be initialized to zero */
    npts = 0;

    if( !wireframetris ) {
      surfaceEvaluator.bgntmesh();
    }
  }
  /**
   * endtmesh - postamble to triangle mesh
   */
  public void endtmesh( ) {
    if( ! wireframetris )
      surfaceEvaluator.endtmesh();
  }
  /**
   * swaptmesh - perform a swap of the triangle mesh pointers
   */
  public void swaptmesh( ) {
    if( wireframetris ) {
      meshindex = 1 - meshindex;
    } else {
      surfaceEvaluator.swaptmesh();
    }
  }
  public void tmeshvert( GridTrimVertex v ) {
    if( v.isGridVert() ) {
      tmeshvert( v.g );
    } else {
      tmeshvert( v.t );
    }
  }
  /**
   * tmeshvert - evaluate a point on a triangle mesh
   */
  public void tmeshvert( TrimVertex t ) {
    long nuid = t.nuid;
    float u = t.param[0];
    float v = t.param[1];

    npts++;
    if( wireframetris ) {
      if( npts >= 3 ) {
        surfaceEvaluator.bgnclosedline();
        if( mesh[0][2] == 0 )
          surfaceEvaluator.evalcoord2f( mesh[0][3], mesh[0][0], mesh[0][1] );
        else
          surfaceEvaluator.evalpoint2i( (long) mesh[0][0], (long) mesh[0][1] );
        if( mesh[1][2] == 0 )
          surfaceEvaluator.evalcoord2f( mesh[1][3], mesh[1][0], mesh[1][1] );
        else
          surfaceEvaluator.evalpoint2i( (long) mesh[1][0], (long) mesh[1][1] );
        surfaceEvaluator.evalcoord2f( nuid, u, v );
        surfaceEvaluator.endclosedline();
      }
      mesh[meshindex][0] = u;
      mesh[meshindex][1] = v;
      mesh[meshindex][2] = 0;
      mesh[meshindex][3] = nuid;
      meshindex = (meshindex+1) % 2;
    } else {
      surfaceEvaluator.evalcoord2f( nuid, u, v );
    }
  }
  /**
   * tmeshvert - evaluate a grid point of a triangle mesh
   */
  public void tmeshvert( GridVertex g ) {
    long u = g->gparam[0];
    long v = g->gparam[1];

    npts++;
    if( wireframetris ) {
      if( npts >= 3 ) {
        surfaceEvaluator.bgnclosedline();
        if( mesh[0][2] == 0 )
          surfaceEvaluator.evalcoord2f( (long) mesh[0][3], mesh[0][0], mesh[0][1] );
        else
          surfaceEvaluator.evalpoint2i( (long) mesh[0][0], (long) mesh[0][1] );
        if( mesh[1][2] == 0 )
          surfaceEvaluator.evalcoord2f( (long) mesh[1][3], mesh[1][0], mesh[1][1] );
        else
          surfaceEvaluator.evalpoint2i( (long) mesh[1][0], (long) mesh[1][1] );
        surfaceEvaluator.evalpoint2i( u, v );
        surfaceEvaluator.endclosedline();
      }
      mesh[meshindex][0] = u;
      mesh[meshindex][1] = v;
      mesh[meshindex][2] = 1;
      meshindex = (meshindex+1) % 2;
    } else {
      surfaceEvaluator.evalpoint2i( u, v );
    }
  }
  /** the same as tmeshvert(trimvertex), for efficiency purpose */
  public void tmeshvert( float u,  float v ) {
    long nuid = 0;
    
    npts++;
    if( wireframetris ) {
      if( npts >= 3 ) {
        surfaceEvaluator.bgnclosedline();
        if( mesh[0][2] == 0 )
          surfaceEvaluator.evalcoord2f( mesh[0][3], mesh[0][0], mesh[0][1] );
        else
          surfaceEvaluator.evalpoint2i( (long) mesh[0][0], (long) mesh[0][1] );
        if( mesh[1][2] == 0 )
          surfaceEvaluator.evalcoord2f( mesh[1][3], mesh[1][0], mesh[1][1] );
        else
          surfaceEvaluator.evalpoint2i( (long) mesh[1][0], (long) mesh[1][1] );
        surfaceEvaluator.evalcoord2f( nuid, u, v );
        surfaceEvaluator.endclosedline();
      }
      mesh[meshindex][0] = u;
      mesh[meshindex][1] = v;
      mesh[meshindex][2] = 0;
      mesh[meshindex][3] = nuid;
      meshindex = (meshindex+1) % 2;
    } else {
      surfaceEvaluator.evalcoord2f( nuid, u, v );
    }
  }
  /**
   * linevert - evaluate a point on an outlined contour
   */
  public void linevert( TrimVertex t ) {
    surfaceEvaluator.evalcoord2f( t.nuid, t.param[0], t.param[1] );
  }
  /**
   * linevert - evaluate a grid point of an outlined contour
   */
  public void linevert( GridVertex g ) {
    surfaceEvaluator.evalpoint2i( g.gparam[0], g.gparam[1] );
  }
  /**
   * bgnoutline - preamble to outlined rendering
   */
  public void bgnoutline( ) {
    surfaceEvaluator.bgnline();
  }
  /**
   * endoutline - postamble to outlined rendering
   */
  public void endoutline( ) {
    surfaceEvaluator.endline();
  }
  /**
   * endsurf - postamble to surface
   */
  public void endsurf( ) {
    surfaceEvaluator.endmap2f();
  }
  /**
   * triangle - output a triangle 
   */
  public void triangle( TrimVertex a, TrimVertex b, TrimVertex c ) {
    bgntfan();
    tmeshvert( a );
    tmeshvert( b );
    tmeshvert( c );
    endtfan();
  }

  public void bgntfan() {
    surfaceEvaluator.bgntfan();
  }
  public void endtfan() {
    surfaceEvaluator.endtfan();
  }
  public void bgnqstrip() {
    surfaceEvaluator.bgnqstrip();
  }
  public void endqstrip() {
    surfaceEvaluator.endqstrip();
  }
  public void evalUStrip(int n_upper, float v_upper, float[] upper_val,
                         int n_lower, float v_lower, float[] lower_val) {
    surfaceEvaluator.evalUStrip(n_upper, v_upper, upper_val, 
                                n_lower, v_lower, lower_val);
  }
  public void evalVStrip(int n_left, float u_left, float[] left_val,
                         int n_right, float v_right, float[] right_val) {
    surfaceEvaluator.evalVStrip(n_left, u_left, left_val,
				n_right, u_right, right_val);
  }
  public void tmeshvertNOGE(TrimVertex *t) {
    // NOTE: under undefined USE_OPTTT #ifdef
  }
  public void tmeshvertNOGE_BU(TrimVertex *t) {
    // NOTE: under undefined USE_OPTTT #ifdef
  }
  public void tmeshvertNOGE_BV(TrimVertex *t) {
    // NOTE: under undefined USE_OPTTT #ifdef
  }
  public void preEvaluateBU(float u) {
    surfaceEvaluator.inPreEvaluateBU_intfac(u);
  }
  public void preEvaluateBV(float v) {
    surfaceEvaluator.inPreEvaluateBV_intfac(v);
  }
 
  /* curve backend routines */
  public void bgncurv( void ) {
    curveEvaluator.bgnmap1f( 0 );
  }
  public void segment( float ulo, float uhi ) {
    curveEvaluator.domain1f( ulo, uhi );
  }
  public void curvpts(long type,    /* geometry, color, texture, normal */
                      float[] pts,  /* control points */
                      long stride,  /* distance to next point */
                      int order,    /* parametric order */
                      float ulo,    /* lower parametric bound */
                      float uhi )   /* upper parametric bound */ {
    curveEvaluator.map1f( type, ulo, uhi, stride, order, pts );
    curveEvaluator.enable( type );
  }
  public void curvgrid( float u0, float u1, long nu ) {
    curveEvaluator.mapgrid1f( nu, u0, u1 );
  }
  public void curvmesh( long from, long n ) {
    curveEvaluator.mapmesh1f( N_MESHFILL, from, from+n );
  }
  public void curvpt( float u ) {
    curveEvaluator.evalcoord1f( 0, u );
  }
  public void bgnline( ) {
    curveEvaluator.bgnline();
  }
  public void endline( ) {
    curveEvaluator.endline();
  }
  public void endcurv( ) {
    curveEvaluator.endmap1f();
  }

  private boolean  wireframetris;
  private boolean  wireframequads;
  private int  npts;
  private float[][] mesh = new float[3][4];
  private int  meshindex;
}
