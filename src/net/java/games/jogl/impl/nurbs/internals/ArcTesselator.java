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

public class ArcTessellator {
  /**
   * Constructs a bezier arc and attaches it to an Arc.
   */
  public void bezier( Arc arc, float s1, float s2, float t1, float t2 ) {
    assert( arc != null );
    assert( ! arc.isTessellated() );

    switch( arc.getside() ) {
      case Arc.SIDE_LEFT:
        assert( s1 == s2 );
        assert( t2 < t1 );
        break;
      case Arc.SIDE_RIGHT:
        assert( s1 == s2 );
        assert( t1 < t2 );
        break;
      case Arc.SIDE_TOP:
        assert( t1 == t2 );
        assert( s2 < s1 );
        break;
      case Arc.SIDE_BOTTOM:
        assert( t1 == t2 );
        assert( s1 < s2 );
        break;
      case Arc.SIDE_NONE:
        throw new InternalError();
        break;
    }
    
    TrimVertex[] p = TrimVertex.allocate(2);
    arc.pwlArc = new PwlArc( p );
    p[0].param[0] = s1;
    p[0].param[1] = t1;
    p[1].param[0] = s2;
    p[1].param[1] = t2;
    assert( (s1 == s2) || (t1 == t2) );
    arc.setbezier();
  }

  /**
   * Constructs a pwl arc and attaches it to an arc.
   */
  public void pwl( Arc arc, float s1, float s2, float t1, float t2, float rate ) {
    int snsteps = 1 + (int) (Math.abs(s2 - s1) / rate );
    int tnsteps = 1 + (int) (Math.abs(t2 - t1) / rate );
    int nsteps = (int) Math.max(1, Math.max( snsteps, tnsteps ));

    float sstepsize = (s2 - s1) / (float) nsteps;
    float tstepsize = (t2 - t1) / (float) nsteps;
    TrimVertex[] newvert = TrimVertex.allocate( nsteps+1 );
    long i;
    for( i = 0; i < nsteps; i++ ) {
	newvert[i].param[0] = s1;
	newvert[i].param[1] = t1;
	s1 += sstepsize;
	t1 += tstepsize;
    }
    newvert[i].param[0] = s2;
    newvert[i].param[1] = t2;

    arc.pwlArc = new PwlArc( newvert );

    arc.clearbezier();
    arc.clearside( );
  }

  /**
   * Constructs a left boundary pwl arc and attaches it to an arc.
   */
  public void pwl_left( Arc arc, float s, float t1, float t2, float rate ) {
    assert( t2 < t1 );

    int nsteps = steps_function(t1, t2, rate);

    float stepsize = (t1 - t2) / (float) nsteps;

    TrimVertex[] newvert = TrimVertex.allocate( nsteps+1 );
    int i;
    for( i = nsteps; i > 0; i-- ) {
	newvert[i].param[0] = s;
	newvert[i].param[1] = t2;
	t2 += stepsize;
    }
    newvert[i].param[0] = s;
    newvert[i].param[1] = t1;

    arc.makeSide( new PwlArc( newvert ), Arc.SIDE_LEFT );
  }

  /**
   * Constructs a right boundary pwl arc and attaches it to an arc.
   */
  public void pwl_right( Arc arc, float s, float t1, float t2, float rate ) {
    assert( t1 < t2 );

    int nsteps = steps_function(t2,t1,rate);
    float stepsize = (t2 - t1) / (float) nsteps;

    TrimVertex[] newvert = TrimVertex.allocate( nsteps+1 );
    int i;
    for( i = 0; i < nsteps; i++ ) {
	newvert[i].param[0] = s;
	newvert[i].param[1] = t1;
	t1 += stepsize;
    }
    newvert[i].param[0] = s;
    newvert[i].param[1] = t2;

    arc.makeSide( new PwlArc( newvert ), Arc.SIDE_RIGHT );
  }

  /**
   * Constructs a top boundary pwl arc and attaches it to an arc.
   */
  public void pwl_top( Arc arc, float t, float s1, float s2, float rate ) {
    assert( s2 < s1 );

    int nsteps = steps_function(s1,s2,rate);
    float stepsize = (s1 - s2) / (float) nsteps;

    TrimVertex[] newvert = TrimVertex.allocate( nsteps+1 );
    int i;
    for( i = nsteps; i > 0; i-- ) {
	newvert[i].param[0] = s2;
	newvert[i].param[1] = t;
	s2 += stepsize;
    }
    newvert[i].param[0] = s1;
    newvert[i].param[1] = t;

    arc.makeSide( new PwlArc( newvert ), Arc.SIDE_TOP );
  }

  /**
   * Constructs a bottom boundary pwl arc and attaches it to an arc.
   */
  public void pwl_bottom( Arc arc, float t, float s1, float s2, float rate ) {
    assert( s1 < s2 );

    int nsteps = steps_function(s2,s1,rate);
    float stepsize = (s2 - s1) / (float) nsteps;

    TrimVertex[] newvert = TrimVertex.allocate( nsteps+1 );
    int i;
    for( i = 0; i < nsteps; i++ ) {
	newvert[i].param[0] = s1;
	newvert[i].param[1] = t;
	s1 += stepsize;
    }
    newvert[i].param[0] = s2;
    newvert[i].param[1] = t;

    arc.makeSide( new PwlArc( newvert ), Arc.SIDE_BOTTOM );
  }

  /**
   * Constucts a linear pwl arc and attaches it to an Arc.
   */
  public void tessellateLinear( Arc arc, float geo_stepsize, float arc_stepsize, boolean isrational ) {
    assert( arc.pwlArc == null );
    float s1, s2, t1, t2;

    //we don't need to scale by arc_stepsize if the trim curve
    //is piecewise linear. Reason: In pwl_right, pwl_left, pwl_top, pwl_left,
    //and pwl, the nsteps is computed by deltaU (or V) /stepsize. 
    //The quantity deltaU/arc_stepsize doesn't have any meaning. And
    //it causes problems: see bug 517641
    float stepsize = geo_stepsize; /* * arc_stepsize*/;

    BezierArc b = arc.bezierArc;

    if( isrational ) {
	s1 = b.cpts[0] / b.cpts[2];
	t1 = b.cpts[1] / b.cpts[2];
	s2 = b.cpts[b.stride+0] / b.cpts[b.stride+2];
	t2 = b.cpts[b.stride+1] / b.cpts[b.stride+2];
    } else {
	s1 = b.cpts[0];
	t1 = b.cpts[1];
	s2 = b.cpts[b.stride+0];
	t2 = b.cpts[b.stride+1];
    }
    if( s1 == s2 )
	if( t1 < t2 )
	    pwl_right( arc, s1, t1, t2, stepsize );
	else
	    pwl_left( arc, s1, t1, t2, stepsize );
    else if( t1 == t2 )
	if( s1 < s2 ) 
	    pwl_bottom( arc, t1, s1, s2, stepsize );
	else
	    pwl_top( arc, t1, s1, s2, stepsize );
    else
	pwl( arc, s1, s2, t1, t2, stepsize );
  }

  /**
   * Constucts a nonlinear pwl arc and attaches it to an Arc.
   */
  public void tessellateNonlinear( Arc arc, float geo_stepsize, float arc_stepsize, int isrational ) {
    assert( arc.pwlArc == null );

    float stepsize = geo_stepsize * arc_stepsize;

    BezierArc *bezierArc = arc.bezierArc;

    float size; //bounding box size of the curve in UV 
    {
      int i,j;
      float min_u, min_v, max_u,max_v;
      min_u = max_u = bezierArc.cpts[0];
      min_v = max_v = bezierArc.cpts[1];
      for(i=1, j=2; i<bezierArc.order; i++, j+= bezierArc.stride)
        {
          if(bezierArc.cpts[j] < min_u)
            min_u = bezierArc.cpts[j];
          if(bezierArc.cpts[j] > max_u)
            max_u = bezierArc.cpts[j];
          if(bezierArc.cpts[j+1] < min_v)
            min_v = bezierArc.cpts[j+1];   
          if(bezierArc.cpts[j+1] > max_v)
            max_v = bezierArc.cpts[j+1]; 
        }

      size = max_u - min_u;
      if(size < max_v - min_v)
        size = max_v - min_v;
    }
      
    /*int nsteps   = 1 + (int) (1.0/stepsize);*/

    int nsteps = (int) (size/stepsize);
    if(nsteps <=0)
      nsteps=1;

    TrimVertex[] vert = TrimVertex.allocate( nsteps+1 );
    float dp   = 1.0/nsteps;
    int vi = 0; // vertIdx

    arc.pwlArc  = new PwlArc();
    arc.pwlArc.pts  = vert;

    if( isrational ) {
      float[] pow_u = new float[Defines.MAXORDER];
      float[] pow_v = new float[Defines.MAXORDER];
      float[] pow_w = new float[Defines.MAXORDER];
      trim_power_coeffs( bezierArc, pow_u, 0 );
      trim_power_coeffs( bezierArc, pow_v, 1 );
      trim_power_coeffs( bezierArc, pow_w, 2 );

      /* compute first point exactly */
      float[] b = bezierArc.cpts;
      vert[vi].param[0] = b[0]/b[2];
      vert[vi].param[1] = b[1]/b[2];

      /* strength reduction on p = dp * step would introduce error */
      long order = bezierArc.order;
      for( int step=1, ++vi; step<nsteps; step++, vi++ ) {
        float p = dp * step;
        float u = pow_u[0];
        float v = pow_v[0];
        float w = pow_w[0];
        for( int i = 1; i < order; i++ ) {
          u = u * p + pow_u[i];
          v = v * p + pow_v[i];
          w = w * p + pow_w[i];
        }
        vert[vi].param[0] = u/w;
        vert[vi].param[1] = v/w;
      }

      /* compute last point exactly */
      b += (order - 1) * bezierArc.stride;
      vert[vi].param[0] = b[0]/b[2];
      vert[vi].param[1] = b[1]/b[2];

    } else {
      float[] pow_u = new float[Defines.MAXORDER];
      float[] pow_v = new float[Defines.MAXORDER];
      trim_power_coeffs( bezierArc, pow_u, 0 );
      trim_power_coeffs( bezierArc, pow_v, 1 );

      /* compute first point exactly */
      float[] b = bezierArc.cpts;
      vert[vi].param[0] = b[0];
      vert[vi].param[1] = b[1];

      /* strength reduction on p = dp * step would introduce error */
      long order =  bezierArc.order;
      for( int step=1, ++vi; step<nsteps; step++, vi++ ) {
        float p = dp * step;
        float u = pow_u[0];
        float v = pow_v[0];
        for( int i = 1; i < bezierArc.order; i++ ) {
          u = u * p + pow_u[i];
          v = v * p + pow_v[i];
        }
        vert[vi].param[0] = u;
        vert[vi].param[1] = v;
      }

      /* compute last point exactly */
      b += (order - 1) * bezierArc.stride;
      vert[vi].param[0] = b[0];
      vert[vi].param[1] = b[1];
    }
    arc.pwlArc.npts = vi + 1;
  }

  private static final float gl_Bernstein[][Defines.MAXORDER][Defines.MAXORDER] = {
    {
      {1, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 }
    },
    {
      {-1, 1, 0, 0, 0, 0, 0, 0 },
      {1, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 }
    },
    {
      {1, -2, 1, 0, 0, 0, 0, 0 },
      {-2, 2, 0, 0, 0, 0, 0, 0 },
      {1, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 }
    },
    {
      {-1, 3, -3, 1, 0, 0, 0, 0 },
      {3, -6, 3, 0, 0, 0, 0, 0 },
      {-3, 3, 0, 0, 0, 0, 0, 0 },
      {1, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 }
    },
    {
      {1, -4, 6, -4, 1, 0, 0, 0 },
      {-4, 12, -12, 4, 0, 0, 0, 0 },
      {6, -12, 6, 0, 0, 0, 0, 0 },
      {-4, 4, 0, 0, 0, 0, 0, 0 },
      {1, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 }
    },
    {
      {-1, 5, -10, 10, -5, 1, 0, 0 },
      {5, -20, 30, -20, 5, 0, 0, 0 },
      {-10, 30, -30, 10, 0, 0, 0, 0 },
      {10, -20, 10, 0, 0, 0, 0, 0 },
      {-5, 5, 0, 0, 0, 0, 0, 0 },
      {1, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 }
    },
    {
      {1, -6, 15, -20, 15, -6, 1, 0 },
      {-6, 30, -60, 60, -30, 6, 0, 0 },
      {15, -60, 90, -60, 15, 0, 0, 0 },
      {-20, 60, -60, 20, 0, 0, 0, 0 },
      {15, -30, 15, 0, 0, 0, 0, 0 },
      {-6, 6, 0, 0, 0, 0, 0, 0 },
      {1, 0, 0, 0, 0, 0, 0, 0 },
      {0, 0, 0, 0, 0, 0, 0, 0 }
    },
    {
      {-1, 7, -21, 35, -35, 21, -7, 1 },
      {7, -42, 105, -140, 105, -42, 7, 0 },
      {-21, 105, -210, 210, -105, 21, 0, 0 },
      {35, -140, 210, -140, 35, 0, 0, 0 },
      {-35, 105, -105, 35, 0, 0, 0, 0 },
      {21, -42, 21, 0, 0, 0, 0, 0 },
      {-7, 7, 0, 0, 0, 0, 0, 0 },
      {1, 0, 0, 0, 0, 0, 0, 0 }
    }
  };

  /**
   * Computes power basis coefficients from bezier coeffients.
   */
  private static void trim_power_coeffs( BezierArc bez_arc, float[] p, int coord ) {
    int stride = bez_arc.stride;
    int order = bez_arc.order;
    float[] base = bez_arc.cpts;
    int baseIdx = coord;

    float[][] mat  = gl_Bernstein[order-1];

    for (int i = 0; i < order; i++) {
      float[] row = mat[i];
      float s = 0.0f;
      int pointIdx = baseIdx;
      for (int j = 0; j < order; j++, pointIdx += stride) {
        s += row[j] * base[pointIdx];
      }
      p[i] = s;
    }
  }
}
