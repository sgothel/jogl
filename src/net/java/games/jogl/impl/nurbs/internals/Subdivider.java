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

public class Subdivider {

  /**
   * Constructs a subdivider.
   */
  public Subdivider(RenderHints hints, Backend b) {
    renderhints = hints;
    arctessellator = new ArcTesselator();
    backend = b;
    slicer = new Slicer(b);
  }

  /**
   * Resets all state after possible error condition.
   */
  public void clear() {
    // FIXME: looks like nothing to do given that we have no object pools
  }
  public void beginTrims() {}
  public void beginLoop() {
    pjarc = 0;
  }

  /**
   * Adds a bezier arc to a trim loop and to a bin.
   */
  public void addArc(float[] cpts, Quilt quilt, long _nuid) {
    BezierArc bezierArc = new BezierArc();
    Arc jarc = new Arc(Arc.SIDE_NONE, _nuid);
    jarc.bezierArc = bezierArc;
    bezierArc.order = quilt.qspec.order;
    bezierArc.stride = quilt.qspec.stride;
    bezierArc.mapdesc = quilt.mapdesc;
    bezierArc.cpts = cpts;
    initialbin.addarc( jarc );
    pjarc = jarc.append( pjarc );
  }

  /**
   * Adds a pwl arc to a trim loop and to a bin.
   */
  public void addArc(int npts, TrimVertex[] pts, long _nuid) {
    Arc jarc = new Arc( Arc.SIDE_NONE, _nuid );
    jarc.pwlArc = new PwlArc( npts, pts );        
    initialbin.addarc( jarc  );
    pjarc = jarc.append( pjarc );
  }
  public void endLoop() {}
  public void endTrims() {}

  public void beginQuilts() {
    qlist = null;
  }
  public void addQuilt( Quilt quilt ) {
    quilt.next = qlist;
    qlist = quilt;
  }
  public void endQuilts() {}

  /**
   * Main curve rendering entry point
   */
  public void drawCurves() {
    float[] from = new float[1];
    float[] to   = new float[1];
    Flist bpts = new Flist();
    qlist.getRange( from, to, bpts );

    renderhints.init( );

    backend.bgncurv();
    float[] pta = new float[0];
    float[] ptb = new float[1];
    for( int i=bpts.start; i<bpts.end-1; i++ ) {
      pta[0] = bpts.pts[i];
      ptb[0] = bpts.pts[i+1];

      qlist.downloadAll( pta, ptb, backend );

      Curvelist curvelist = new Curvelist( qlist, pta, ptb );
      samplingSplit( curvelist, renderhints.maxsubdivisions );
    }
    backend.endcurv();
  }
  public void drawSurfaces(long nuid) {
    renderhints.init( );

    if (qlist == null) {
      //initialbin could be nonempty due to some errors
      freejarcs(initialbin);
      return;
    }

    for( Quilt q = qlist; q != null; q = q.next ) {
      if( q.isCulled( ) == Defines.CULL_TRIVIAL_REJECT ) {
        freejarcs( initialbin );
        return;
      }
    }


    float[] from = new float[2];
    float[] to   = new float[2];
    qlist.getRange( from, to, spbrkpts, tpbrkpts );
    //perform optimization only when the samplng method is 
    //DOMAIN_DISTANCE and the display methdo is either 
    //fill or outline_polygon.
    bool optimize = (is_domain_distance_sampling && (renderhints.display_method != N_OUTLINE_PATCH));

    if( ! initialbin.isnonempty() ) {
      if(! optimize )
        {  
          makeBorderTrim( from, to );
        }
    } else {
      float[] rate = new float[2];
      qlist.findRates( spbrkpts, tpbrkpts, rate );

      if( decompose( initialbin, Math.min(rate[0], rate[1]) ) ) 
        throw new NurbsException( 31 );
    }

    backend.bgnsurf( renderhints.wiretris, renderhints.wirequads, nuid );

    if( (!initialbin.isnonempty())  && optimize )
      {
        int i,j;
        int num_u_steps;
        int num_v_steps;
        for(i=spbrkpts.start; i<spbrkpts.end-1; i++){
          for(j=tpbrkpts.start; j<tpbrkpts.end-1; j++){
            float[] pta = new float[2];
            float[] ptb = new float[2];
            pta[0] = spbrkpts.pts[i];
            ptb[0] = spbrkpts.pts[i+1];
            pta[1] = tpbrkpts.pts[j];
            ptb[1] = tpbrkpts.pts[j+1];
            qlist.downloadAll(pta, ptb, backend);
     
            num_u_steps = (int) (domain_distance_u_rate * (ptb[0]-pta[0]));
            num_v_steps = (int) (domain_distance_v_rate * (ptb[1]-pta[1]));

            if(num_u_steps <= 0) num_u_steps = 1;
            if(num_v_steps <= 0) num_v_steps = 1;

            backend.surfgrid(pta[0], ptb[0], num_u_steps, 
                             ptb[1], pta[1], num_v_steps);
            backend.surfmesh(0,0,num_u_steps,num_v_steps);

            continue;
          }
        }
      }
    else
      subdivideInS( initialbin );

    backend.endsurf();
  }

  public int ccwTurn_sl(Arc j1, Arc j2 ) {
    int v1i             = j1.pwlArc.npts-1;
    int v1lasti         = 0;
    int v2i             = 0;
    int v2lasti         = j2.pwlArc.npts-1;
    int v1nexti         = v1i-1;
    int v2nexti         = v2i+1;
    TrimVertex v1       = j1.pwlArc.pts[v1i];
    TrimVertex v1last   = j1.pwlArc.pts[v1lasti];
    TrimVertex v2       = j2.pwlArc.pts[v2i];
    TrimVertex v2last   = j2.pwlArc.pts[v2lasti];
    TrimVertex v1next   = j1.pwlArc.pts[v1nexti];
    TrimVertex v2next   = j2.pwlArc.pts[v2nexti];
    int sgn;

    assert( v1 != v1last );
    assert( v2 != v2last );

    // the arcs lie on the line (0 == v1.param[0])
    if( v1.param[0] == v1next.param[0] && v2.param[0] == v2next.param[0] )
      return 0;

    if( v2next.param[0] > v2.param[0] || v1next.param[0] > v1.param[0] ) 
      throw new NurbsException(28);

    if( v1.param[1] < v2.param[1] )
      return 1;
    else if( v1.param[1] > v2.param[1] )
      return 0;

    while( true ) {
      if( v1next.param[0] > v2next.param[0] ) {
        assert( v1.param[0] >= v1next.param[0] );
        assert( v2.param[0] >= v1next.param[0] );
        switch( bbox( v2next, v2, v1next, 1 ) ) {
        case -1:
          return 1;
        case 0:
          sgn = ccw( v1next, v2, v2next );
          if( sgn != -1 ) 
            return sgn;
          else {
            v1i = v1nexti--;
            v1 = j1.pwlArc.pts[v1i];
            v1next = j1.pwlArc.pts[v1nexti];
            if( v1 == v1last ) {
              return 0; // ill-conditioned, guess answer
            }
          }
          break;
        case 1:
          return 0;
        }
      } else if( v1next.param[0] < v2next.param[0] ) {
        assert( v1.param[0] >= v2next.param[0] );
        assert( v2.param[0] >= v2next.param[0] );
        switch( bbox( v1next, v1, v2next, 1 ) ) {
        case -1:
          return 0;
        case 0:
          sgn = ccw( v1next, v1, v2next );
          if( sgn != -1 ) 
            return sgn;
          else {
            v2i = v2nexti++;
            v2 = j2.pwlArc.pts[v2i];
            v2next = j2.pwlArc.pts[v2nexti];
            if( v2 == v2last ) {
              return 0; // ill-conditioned, guess answer
            }
          }
          break;
        case 1:
          return 1;
        }
      } else {
        if( v1next.param[1] < v2next.param[1] )
          return 1;
        else if( v1next.param[1] > v2next.param[1] )
          return 0;
        else {
          v2i = v2nexti++;
          v2 = j2.pwlArc.pts[v2i];
          v2next = j2.pwlArc.pts[v2nexti];
          if( v2 == v2last ) {
            return 0; // ill-conditioned, guess answer
          }
        }
      }
    }
  }

  public int ccwTurn_sr(Arc j1, Arc j2 ) {
    // dir = 1
    int v1i             = j1.pwlArc.npts-1;
    int v1lasti         = 0;
    int v2i             = 0;
    int v2lasti         = j2.pwlArc.npts-1;
    int v1nexti         = v1i-1;
    int v2nexti         = v2i+1;
    TrimVertex v1       = j1.pwlArc.pts[v1i];
    TrimVertex v1last   = j1.pwlArc.pts[v1lasti];
    TrimVertex v2       = j2.pwlArc.pts[v2i];
    TrimVertex v2last   = j2.pwlArc.pts[v2lasti];
    TrimVertex v1next   = j1.pwlArc.pts[v1nexti];
    TrimVertex v2next   = j2.pwlArc.pts[v2nexti];
    int sgn;

    assert( v1 != v1last );
    assert( v2 != v2last );

    // the arcs lie on the line (0 == v1.param[0])
    if( v1.param[0] == v1next.param[0] && v2.param[0] == v2next.param[0] )
      return 0;

    if( v2next.param[0] < v2.param[0] || v1next.param[0] < v1.param[0] )
      throw new NurbsException(28);

    if( v1.param[1] < v2.param[1] )
      return 0;
    else if( v1.param[1] > v2.param[1] )
      return 1;

    while( true ) {
      if( v1next.param[0] < v2next.param[0] ) {
        assert( v1.param[0] <= v1next.param[0] );
        assert( v2.param[0] <= v1next.param[0] );
        switch( bbox( v2, v2next, v1next, 1 ) ) {
        case -1:
          return 0;
        case 0:
          sgn = ccw( v1next, v2, v2next );
          if( sgn != -1 ) {
            return sgn;
          } else {
            v1i = v1nexti--;
            v1 = j1.pwlArc.pts[v1i];
            v1next = j1.pwlArc.pts[v1nexti];
            if( v1 == v1last ) {
              return 0; // ill-conditioned, guess answer
            }
          }
          break;
        case 1:
          return 1;
        }
      } else if( v1next.param[0] > v2next.param[0] ) {
        assert( v1.param[0] <= v2next.param[0] );
        assert( v2.param[0] <= v2next.param[0] );
        switch( bbox( v1, v1next, v2next, 1 ) ) {
        case -1:
          return 1;
        case 0:
          sgn = ccw( v1next, v1, v2next );
          if( sgn != -1 ) { 
            return sgn;
          } else {
            v2i = v2nexti++;
            v2 = j2.pwlArc.pts[v2i];
            v2next = j2.pwlArc.pts[v2nexti];
            if( v2 == v2last ) {
              return 0; // ill-conditioned, guess answer
            }
          }
          break;
        case 1:
          return 0;
        }
      } else {
        if( v1next.param[1] < v2next.param[1] )
          return 0;
        else if( v1next.param[1] > v2next.param[1] )
          return 1;
        else {
          v2i = v2nexti++;
          v2 = j2.pwlArc.pts[v2i];
          v2next = j2.pwlArc.pts[v2nexti];
          if( v2 == v2last ) {
            return 0; // ill-conditioned, guess answer
          }
        }
      }
    }
  }

  public int ccwTurn_tl(Arc j1, Arc j2 ) {
    int v1i             = j1.pwlArc.npts-1;
    int v1lasti         = 0;
    int v2i             = 0;
    int v2lasti         = j2.pwlArc.npts-1;
    int v1nexti         = v1i-1;
    int v2nexti         = v2i+1;
    TrimVertex v1       = j1.pwlArc.pts[v1i];
    TrimVertex v1last   = j1.pwlArc.pts[v1lasti];
    TrimVertex v2       = j2.pwlArc.pts[v2i];
    TrimVertex v2last   = j2.pwlArc.pts[v2lasti];
    TrimVertex v1next   = j1.pwlArc.pts[v1nexti];
    TrimVertex v2next   = j2.pwlArc.pts[v2nexti];
    int sgn;

    assert( v1 != v1last );
    assert( v2 != v2last );

    // the arcs lie on the line (1 == v1.param[1])
    if( v1.param[1] == v1next.param[1] && v2.param[1] == v2next.param[1] )
      return 0;

    if( v2next.param[1] > v2.param[1] || v1next.param[1] > v1.param[1] ) 
      throw new NurbsException(28 );

    if( v1.param[0] < v2.param[0] )
      return 0;
    else if( v1.param[0] > v2.param[0] )
      return 1;

    while( true ) {
      if( v1next.param[1] > v2next.param[1] ) {
        assert( v1.param[1] >= v1next.param[1] );
        assert( v2.param[1] >= v1next.param[1] );
        switch( bbox( v2next, v2, v1next, 0 ) ) {
        case -1:
          return 0;
        case 0:
          sgn = ccw( v1next, v2, v2next );
          if( sgn != -1 ) 
            return sgn;
          else {
            v1i = v1nexti--;
            v1 = j1.pwlArc.pts[v1i];
            v1next = j1.pwlArc.pts[v1nexti];
            if( v1 == v1last ) {
              return 0; // ill-conditioned, guess answer
            }
          }
          break;
        case 1:
          return 1;
        }
      } else if( v1next.param[1] < v2next.param[1] ) {
        switch( bbox( v1next, v1, v2next, 0 ) ) {
        case -1:
          return 1;
        case 0:
          sgn = ccw( v1next, v1, v2next );
          if( sgn != -1 ) 
            return sgn;
          else {
            v2i = v2nexti++;
            v2 = j2.pwlArc.pts[v2i];
            v2next = j2.pwlArc.pts[v2nexti];
            if( v2 == v2last ) {
              return 0; // ill-conditioned, guess answer
            }
          }
          break;
        case 1:
          return 0;
        }
      } else {
        if( v1next.param[0] < v2next.param[0] )
          return 0;
        else if( v1next.param[0] > v2next.param[0] )
          return 1;
        else {
          v2i = v2nexti++;
          v2 = j2.pwlArc.pts[v2i];
          v2next = j2.pwlArc.pts[v2nexti];
          if( v2 == v2last ) {
            return 0; // ill-conditioned, guess answer
          }
        }
      }
    }
  }

  public int ccwTurn_tr(Arc j1, Arc j2) {
    int v1i             = j1.pwlArc.npts-1;
    int v1lasti         = 0;
    int v2i             = 0;
    int v2lasti         = j2.pwlArc.npts-1;
    int v1nexti         = v1i-1;
    int v2nexti         = v2i+1;
    TrimVertex v1       = j1.pwlArc.pts[v1i];
    TrimVertex v1last   = j1.pwlArc.pts[v1lasti];
    TrimVertex v2       = j2.pwlArc.pts[v2i];
    TrimVertex v2last   = j2.pwlArc.pts[v2lasti];
    TrimVertex v1next   = j1.pwlArc.pts[v1nexti];
    TrimVertex v2next   = j2.pwlArc.pts[v2nexti];
    int sgn;

    assert( v1 != v1last );
    assert( v2 != v2last );

    // the arcs lie on the line (1 == v1.param[1])
    if( v1.param[1] == v1next.param[1] && v2.param[1] == v2next.param[1] )
      return 0;

    if( v2next.param[1] < v2.param[1] || v1next.param[1] < v1.param[1] )
      throw new NurbsException( 28 );

    if( v1.param[0] < v2.param[0] )
      return 1;
    else if( v1.param[0] > v2.param[0] )
      return 0;

    while( 1 ) {
      if( v1next.param[1] < v2next.param[1] ) {
        assert( v1.param[1] <= v1next.param[1] );
        assert( v2.param[1] <= v1next.param[1] );
        switch( bbox( v2, v2next, v1next, 0 ) ) {
        case -1:
          return 1;
        case 0:
          sgn = ccw( v1next, v2, v2next );
          if( sgn != -1 ) {
            return sgn;
          } else {
            v1i = v1nexti--;
            v1 = j1.pwlArc.pts[v1i];
            v1next = j1.pwlArc.pts[v1nexti];
            if( v1 == v1last ) {
              return 0; // ill-conditioned, guess answer
            }
          }
          break;
        case 1:
          return 0;
        }
      } else if( v1next.param[1] > v2next.param[1] ) {
        assert( v1.param[1] <= v2next.param[1] );
        assert( v2.param[1] <= v2next.param[1] );
        switch( bbox( v1, v1next, v2next, 0 ) ) {
        case -1:
          return 0;
        case 0:
          sgn = ccw( v1next, v1, v2next );
          if( sgn != -1 ) { 
            return sgn;
          } else {
            v2i = v2nexti++;
            v2 = j2.pwlArc.pts[v2i];
            v2next = j2.pwlArc.pts[v2nexti];
            if( v2 == v2last ) {
              return 0; // ill-conditioned, guess answer
            }
          }
          break;
        case 1:
          return 1;
        }
      } else {
        if( v1next.param[0] < v2next.param[0] )
          return 1;
        else if( v1next.param[0] > v2next.param[0] )
          return 0;
        else {
          v2i = v2nexti++;
          v2 = j2.pwlArc.pts[v2i];
          v2next = j2.pwlArc.pts[v2nexti];
          if( v2 == v2last ) {
            return 0; // ill-conditioned, guess answer
          }
        }
      }
    }
  }

  public void set_domain_distance_u_rate(float u_rate) {
    domain_distance_u_rate = u_rate;
  }

  public void set_domain_distance_v_rate(float v_rate) {
    domain_distance_v_rate = v_rate;
  }

  public void set_is_domain_distance_sampling(int flag) {
    is_domain_distance_sampling = flag;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  /**
   * Determine which side of a line a jarc lies (for debugging only)
   */
  private int arc_classify( Arc jarc, int param, float value )
  {
    float tdiff, hdiff;
    if( param == 0 ) {
      tdiff = jarc.tail()[0] - value;
      hdiff = jarc.head()[0] - value;
    } else {
      tdiff = jarc.tail()[1] - value;
      hdiff = jarc.head()[1] - value;
    }

    if( tdiff > 0.0 ) {
      if( hdiff > 0.0 ) {
        return 0x11;
      } else if( hdiff == 0.0 ) {
        return 0x12;
      } else {
        return 0x10;
      }
    } else if( tdiff == 0.0 ) {
      if( hdiff > 0.0 ) {
        return 0x21;
      } else if( hdiff == 0.0 ) {
        return 0x22;
      } else {
        return 0x20;
      }
    } else {
      if( hdiff > 0.0 ) {
        return 0x01;
      } else if( hdiff == 0.0 ) {
        return 0x02;
      } else {
        return 0;
      }
    }
  }

  private void classify_headonleft_s( Bin bin, Bin in, Bin out, float val ) {
    /* tail on line, head at left */
    Arc j;

    while( (j = bin.removearc()) != null ) {
      assert( arc_classify( j, 0, val ) == 0x20 );

      j.setitail();

      float diff = j.prev.tail()[0] - val;
      if( diff > 0.0 ) {
        out.addarc( j );
      } else if( diff < 0.0 ) {
        if( ccwTurn_sl( j.prev, j ) != 0 )
          out.addarc( j );
        else
          in.addarc( j );
      } else {
        if( j.prev.tail()[1] > j.prev.head()[1] )
          in.addarc( j );
        else
          out.addarc( j );
      }
    }
  }

  private void classify_tailonleft_s( Bin bin, Bin in, Bin out, float val ) {
    /* tail at left, head on line */
    Arc j;

    while( (j = bin.removearc()) != null ) {
      assert( arc_classify( j, 1, val ) == 0x02 );
      j.clearitail();

      float diff = j.next.head()[1] - val;
      if( diff > 0.0 ) {
        in.addarc( j );
      } else if( diff < 0.0 ) {
        if( ccwTurn_tl( j, j.next ) != 0 )
          out.addarc( j );
        else
          in.addarc( j );
      } else {
        if (j.next.tail()[0] > j.next.head()[0] )
          out.addarc( j );
        else
          in.addarc( j );
      }
    }
  }

  private void classify_headonright_s( Bin bin, Bin in, Bin out, float val ) {
    /* tail on line, head at right */
    Arc j;

    while( (j = bin.removearc()) != null ) {
      assert( arc_classify( j, 0, val ) == 0x21 );
    
      j.setitail();

      float diff = j.prev.tail()[0] - val;
      if( diff > 0.0 ) { 
        if( ccwTurn_sr( j.prev, j ) != 0 )
          out.addarc( j );
        else
          in.addarc( j );
      } else if( diff < 0.0 ) {
        out.addarc( j );
      } else {
        if( j.prev.tail()[1] > j.prev.head()[1] )
          out.addarc( j );
        else
          in.addarc( j );
      }
    }
  }

  private void classify_tailonright_s( Bin bin, Bin in, Bin out, float val ) {
    /* tail at right, head on line */
    Arc j;

    while( (j = bin.removearc()) != null ) {
      assert( arc_classify( j, 0, val ) == 0x12);
	
      j.clearitail();

      float diff = j.next.head()[0] - val;
      if( diff > 0.0 ) {
        if( ccwTurn_sr( j, j.next ) != 0 )
          out.addarc( j );
        else
          in.addarc( j );
      } else if( diff < 0.0 ) {
        in.addarc( j );
      } else {
        if( j.next.tail()[1] > j.next.head()[1] ) 
          out.addarc( j );
        else
          in.addarc( j );
      }
    }
  }

  private void classify_headonleft_t( Bin bin, Bin in, Bin out, float val ) {
    /* tail on line, head at left */
    Arc j;

    while( (j = bin.removearc()) != null ) {
      assert( arc_classify( j, 1, val ) == 0x20 );
      j.setitail();

      float diff = j.prev.tail()[1] - val;
      if( diff > 0.0 ) {
        out.addarc( j );
      } else if( diff < 0.0 ) {
        if( ccwTurn_tl( j.prev, j ) != 0 )
          out.addarc( j );
        else
          in.addarc( j );
      } else {
        if( j.prev.tail()[0] > j.prev.head()[0] )
          out.addarc( j );
        else
          in.addarc( j );
      }
    }
  }

  private void classify_tailonleft_t( Bin bin, Bin in, Bin out, float val ) {
    /* tail at left, head on line */
    Arc j;

    while( (j = bin.removearc()) != null ) {
      assert( arc_classify( j, 1, val ) == 0x02 );
      j.clearitail();

      float diff = j.next.head()[1] - val;
      if( diff > 0.0 ) {
        in.addarc( j );
      } else if( diff < 0.0 ) {
        if( ccwTurn_tl( j, j.next ) != 0 )
          out.addarc( j );
        else
          in.addarc( j );
      } else {
        if (j.next.tail()[0] > j.next.head()[0] )
          out.addarc( j );
        else
          in.addarc( j );
      }
    }
  }

  private void classify_headonright_t( Bin bin, Bin in, Bin out, float val ) {
    /* tail on line, head at right */
    Arc j;

    while( (j = bin.removearc()) != null ) {
      assert( arc_classify( j, 1, val ) == 0x21 );
    
      j.setitail();

      float diff = j.prev.tail()[1] - val;
      if( diff > 0.0 ) { 
        if( ccwTurn_tr( j.prev, j ) != 0 )
          out.addarc( j );
        else
          in.addarc( j );
      } else if( diff < 0.0 ) {
        out.addarc( j );
      } else {
        if( j.prev.tail()[0] > j.prev.head()[0] )
          in.addarc( j );
        else
          out.addarc( j );
      }
    }
  }

  private void classify_tailonright_t( Bin bin, Bin in, Bin out, float val ) {
    /* tail at right, head on line */
    Arc j;

    while( (j = bin.removearc()) != null ) {
      assert( arc_classify( j, 1, val ) == 0x12);
	
      j.clearitail();

      float diff =  j.next.head()[1] - val;
      if( diff > 0.0 ) {
        if( ccwTurn_tr( j, j.next ) != 0 )
          out.addarc( j );
        else
          in.addarc( j );
      } else if( diff < 0.0 ) { 
        in.addarc( j );
      } else {
        if( j.next.tail()[0] > j.next.head()[0] ) 
          in.addarc( j );
        else
          out.addarc( j );
      }
    }
  }

  private int DIR_DOWN = 0;
  private int DIR_SAME = 1;
  private int DIR_UP   = 2;
  private int DIR_NONE = 3;

  private void tessellate( Arc_ptr, float );
  private void monotonize( Arc_ptr , Bin & );
  private int isMonotone( Arc_ptr  );
  private int decompose( Bin &, float );


  private Slicer slicer;
  private ArcTessellator arctessellator;
  //  private Pool arcpool;
  //  private Pool bezierarcpool;
  //  private Pool pwlarcpool;
  //  private TrimVertexPool trimvertexpool;

  private JumpBuffer* jumpbuffer;
  private Renderhints& renderhints;
  private Backend&  backend;

  private Bin initialbin;
  private Arc pjarc;
  private int s_index;
  private int t_index;
  private Quilt *qlist;
  private Flist spbrkpts;
  private Flist tpbrkpts;
  private Flist smbrkpts;
  private Flist tmbrkpts;
  private float stepsizes[4];
  private int showDegenerate;
  private int isArcTypeBezier;

  // FIXME: NOT FINISHED
  private void samplingSplit( Curvelist&, int );

  private void subdivideInS( Bin source ) {
    if( renderhints.display_method == N_OUTLINE_PARAM ) {
      outline( source );
      freejarcs( source );
    } else {
      setArcTypeBezier();
      setNonDegenerate();
      splitInS( source, spbrkpts.start, spbrkpts.end );
    }
  }

  /**
   * Splits a patch and a bin by an isoparametric line.
   */
  private void splitInS( Bin source, int start, int end ) {
    if( source.isnonempty() ) {
      if( start != end ) {
        int  i = start + (end - start) / 2;
        Bin left = new Bin();
        Bin right = new Bin();
        split( source, left, right, 0, spbrkpts.pts[i] );
        splitInS( left, start, i );
        splitInS( right, i+1, end );
      } else {
        if( start == spbrkpts.start || start == spbrkpts.end ) {
          freejarcs( source );
        } else if( renderhints.display_method == NurbsConsts.N_OUTLINE_PARAM_S ) {
          outline( source );
          freejarcs( source );
        } else {
          setArcTypeBezier();
          setNonDegenerate();
          s_index = start;
          splitInT( source, tpbrkpts.start, tpbrkpts.end );
        }
      }
    } 
  }

  /**
   * Splits a patch and a bin by an isoparametric line.
   */
  private void splitInT( Bin source, int start, int end ) {
    if( source.isnonempty() ) {
      if( start != end ) {
        int  i = start + (end - start) / 2;
        Bin left = new Bin();
        Bin right = new Bin();
        split( source, left, right, 1, tpbrkpts.pts[i] );
        splitInT( left, start, i );
        splitInT( right, i+1, end );
      } else {
        if( start == tpbrkpts.start || start == tpbrkpts.end ) {
          freejarcs( source );
        } else if( renderhints.display_method == NurbsConsts.N_OUTLINE_PARAM_ST ) {
          outline( source );
          freejarcs( source );
        } else {
          t_index = start;
          setArcTypeBezier();
          setDegenerate();

          float[] pta = new float[2];
          float[] ptb = new float[2];
          pta[0] = spbrkpts.pts[s_index-1];
          pta[1] = tpbrkpts.pts[t_index-1];

          ptb[0] = spbrkpts.pts[s_index];
          ptb[1] = tpbrkpts.pts[t_index];
          qlist.downloadAll( pta, ptb, backend );
      
          Patchlist patchlist = new Patchlist( qlist, pta, ptb );
          /*
            printf("-------samplingSplit-----\n");
            source.show("samplingSplit source");
          */
          samplingSplit( source, patchlist, renderhints.maxsubdivisions, 0 );
          setNonDegenerate();
          setArcTypeBezier();
        }
      }
    } 
  }

  /**
   * Recursively subdivides patch, cull checks each subpatch  
   */
  private void samplingSplit( Bin source, Patchlist patchlist, int subdivisions, int param ) {
    if( ! source.isnonempty() ) return;

    if( patchlist.cullCheck() == Defines.CULL_TRIVIAL_REJECT ) {
      freejarcs( source );
      return;
    }

    patchlist.getstepsize();

    if( renderhints.display_method == NurbsConsts.N_OUTLINE_PATCH ) {
      tessellation( source, patchlist );
      outline( source );
      freejarcs( source );
      return;
    } 

    //patchlist.clamp();

    tessellation( source, patchlist );

    if( patchlist.needsSamplingSubdivision() && (subdivisions > 0) ) {
      if( ! patchlist.needsSubdivision( 0 ) )
        param = 1;
      else if( ! patchlist.needsSubdivision( 1 ) )
        param = 0;
      else
        param = 1 - param;

      Bin left = new Bin();
      Bin right = new Bin();
      float mid = ( patchlist.pspec[param].range[0] +
                   patchlist.pspec[param].range[1] ) * 0.5;
      split( source, left, right, param, mid );
      Patchlist subpatchlist = new Patchlist( patchlist, param, mid );
      samplingSplit( left, subpatchlist, subdivisions-1, param );
      samplingSplit( right, patchlist, subdivisions-1, param );
    } else {
      setArcTypePwl();
      setDegenerate();
      nonSamplingSplit( source, patchlist, subdivisions, param );
      setDegenerate();
      setArcTypeBezier();
    }
  }

  private void nonSamplingSplit( Bin source, Patchlist patchlist, int subdivisions, int param ) {
    if( patchlist.needsNonSamplingSubdivision() && (subdivisions > 0) ) {
      param = 1 - param;

      Bin left = new Bin();
      Bin right = new Bin();
      float mid = ( patchlist.pspec[param].range[0] +
                   patchlist.pspec[param].range[1] ) * 0.5;
      split( source, left, right, param, mid );
      Patchlist subpatchlist = new Patchlist( patchlist, param, mid );
      if( left.isnonempty() )
        if( subpatchlist.cullCheck() == Defines.CULL_TRIVIAL_REJECT ) 
          freejarcs( left );
        else
          nonSamplingSplit( left, subpatchlist, subdivisions-1, param );
      if( right.isnonempty() ) 
        if( patchlist.cullCheck() == Defines.CULL_TRIVIAL_REJECT ) 
          freejarcs( right );
        else
          nonSamplingSplit( right, patchlist, subdivisions-1, param );

    } else {
      // make bbox calls
      patchlist.bbox();
      backend.patch( patchlist.pspec[0].range[0], patchlist.pspec[0].range[1],
                     patchlist.pspec[1].range[0], patchlist.pspec[1].range[1] );
    
      if( renderhints.display_method == NurbsConsts.N_OUTLINE_SUBDIV ) {
        outline( source );
        freejarcs( source );
      } else {
        setArcTypePwl();
        setDegenerate();
        findIrregularS( source );
        monosplitInS( source, smbrkpts.start, smbrkpts.end );
      }
    }
  }

  /**
   * Sets tessellation of interior and boundary of patch.
   */
  private void tessellation( Bin bin, Patchlist patchlist ) {
    // tessellate unsampled trim curves
    tessellate( bin, patchlist.pspec[1].sidestep[1], patchlist.pspec[0].sidestep[1],
                patchlist.pspec[1].sidestep[0], patchlist.pspec[0].sidestep[0] );

    // set interior sampling rates
    slicer.setstriptessellation( patchlist.pspec[0].stepsize, patchlist.pspec[1].stepsize );

    //added by zl: set the order which will be used in slicer.c++
    slicer.set_ulinear( (patchlist.get_uorder() == 2));
    slicer.set_vlinear( (patchlist.get_vorder() == 2));

    // set boundary sampling rates
    stepsizes[0] = patchlist.pspec[1].stepsize;
    stepsizes[1] = patchlist.pspec[0].stepsize;
    stepsizes[2] = patchlist.pspec[1].stepsize;
    stepsizes[3] = patchlist.pspec[0].stepsize;
  }

  /**
   * Splits a patch and a bin by an isoparametric line.
   */
  private void monosplitInS( Bin source, int start, int end ) {
    if( source.isnonempty() ) {
      if( start != end ) {
        int  i = start + (end - start) / 2;
        Bin left = new Bin();
        Bin right = new Bin();
        split( source, left, right, 0, smbrkpts.pts[i] );
        monosplitInS( left, start, i );
        monosplitInS( right, i+1, end );
      } else {
        if( renderhints.display_method == NurbsConsts.N_OUTLINE_SUBDIV_S ) {
          outline( source );
          freejarcs( source );
        } else {
          setArcTypePwl();
          setDegenerate();
          findIrregularT( source );
          monosplitInT( source, tmbrkpts.start, tmbrkpts.end );
        }
      }
    } 
  }

  /**
   * Splits a patch and a bin by an isoparametric line.
   */
  private void monosplitInT( Bin source, int start, int end ) {
    if( source.isnonempty() ) {
      if( start != end ) {
        int  i = start + (end - start) / 2;
        Bin left = new Bin();
        Bin right = new Bin();
        split( source, left, right, 1, tmbrkpts.pts[i] );
        monosplitInT( left, start, i );
        monosplitInT( right, i+1, end );
      } else {
        if( renderhints.display_method == NurbsConsts.N_OUTLINE_SUBDIV_ST ) {
          outline( source );
          freejarcs( source );
        } else {
          /*
            printf("*******render\n");
            source.show("source\n");
          */
          render( source );
          freejarcs( source );
        }
      }
    } 
  }

  /**
   * Renders the trimmed patch by outlining the boundary .
   */
  private void outline( Bin bin ) {
    bin.markall();
    for( Arc jarc=bin.firstarc(); jarc != null; jarc=bin.nextarc() ) {
      if( jarc.ismarked() ) {
        assert( jarc.check( ) );
        Arc jarchead = jarc;
        do {
          slicer.outline( jarc );
          jarc.clearmark();
          jarc = jarc.prev;
        } while (jarc != jarchead);
      }
    }
  }

  /**
   * Frees all arcs in a bin.
   */
  private void freejarcs( Bin & ) {
    bin.adopt();  /* XXX - should not be necessary */

    Arc jarc;
    while( (jarc = bin.removearc()) != null ) {
      if( jarc.pwlArc != null ) jarc.pwlArc.deleteMe( ); jarc.pwlArc = null;
      if( jarc.bezierArc != null) jarc.bezierArc.deleteMe( ); jarc.bezierArc = null;
      jarc.deleteMe( );
    }
  }

  /**
   * Renders all monotone regions in a bin and frees the bin.
   */
  private void render( Bin bin ) {
    bin.markall();

    slicer.setisolines( ( renderhints.display_method == N_ISOLINE_S ) ? 1 : 0 );

    for( Arc jarc=bin.firstarc(); jarc != null; jarc=bin.nextarc() ) {
      if( jarc.ismarked() ) {
        assert( jarc.check( ) != 0 );
        Arc jarchead = jarc;
        do {
          jarc.clearmark();
          jarc = jarc.next;
        } while (jarc != jarchead);
        slicer.slice( jarc );
      }
    }
  }

  private void split( Bin &, Bin &, Bin &, int, float );

  /**
   * Tessellates all Bezier arcs in a bin.
   * <ol>
   *   <li> only accepts linear Bezier arcs as input 
   *   <li> the Bezier arcs are stored in the pwlArc structure
   *   <li> only vertical or horizontal lines work
   * </ol>
   * should:
   * <ol>
   *   <li> represent Bezier arcs in BezierArc structure
   *        (this requires a multitude of changes to the code)
   *   <li> accept high degree Bezier arcs (hard)
   *   <il> map the curve onto the surface to determine tessellation
   *   <li> work for curves of arbitrary geometry
   * </ol>
   *----------------------------------------------------------------------------
   */
  private void tessellate( Bin bin, float rrate, float trate, float lrate, float brate ) {
    for( Arc jarc=bin.firstarc(); jarc != null; jarc=bin.nextarc() ) {
      if( jarc.isbezier( ) ) {
        assert( jarc.pwlArc.npts == 2 );  
        TrimVertex[] pts = jarc.pwlArc.pts;
        float s1 = pts[0].param[0];
        float t1 = pts[0].param[1];
        float s2 = pts[1].param[0];
        float t2 = pts[1].param[1];
      
        jarc.pwlArc.deleteMe( );
        jarc.pwlArc = null;
      
        switch( jarc.getside() ) {
        case Arc.SIDE_LEFT:
          assert( s1 == s2 );
          arctessellator.pwl_left( jarc, s1, t1, t2, lrate );
          break;
        case Arc.SIDE_RIGHT:
          assert( s1 == s2 );
          arctessellator.pwl_right( jarc, s1, t1, t2, rrate );
          break;
        case Arc.SIDE_TOP:
          assert( t1 == t2 );
          arctessellator.pwl_top( jarc, t1, s1, s2, trate );
          break;
        case Arc.SIDE_BOTTOM:
          assert( t1 == t2 );
          arctessellator.pwl_bottom( jarc, t1, s1, s2, brate );
          break;
        case Arc.SIDE_NONE:
          throw new InternalError("Incorrect tesselation state");
          break;
        }
        assert( ! jarc.isbezier() );
        assert( jarc.check() != 0 );
      }
    }
  }

  private inline void setDegenerate( void ) { showDegenerate = 1; }
  private inline void setNonDegenerate( void ) { showDegenerate = 0; }
  private inline int showingDegenerate( void ) { return showDegenerate; }
  private inline void setArcTypeBezier( void ) { isArcTypeBezier = 1; }
  private inline void setArcTypePwl( void ) { isArcTypeBezier = 0; }
  private inline int isBezierArcType( void ) { return isArcTypeBezier; }

  /**
   * If no user input trimming data, then create a trimming curve
   * around the boundaries of the Quilt. The curve consists of four
   * Jordan arcs, one for each side of the Quilt, connected, of
   * course, head to tail.
   */
  private void makeBorderTrim( float[] from, float[] to ) {
    float smin = from[0];
    float smax = to[0];
    float tmin = from[1];
    float tmax = to[1];

    pjarc = null;

    Arc jarc = new Arc( Arc.SIDE_BOTTOM, 0 );
    arctessellator.bezier( jarc, smin, smax, tmin, tmin );
    initialbin.addarc( jarc  );
    pjarc = jarc.append( pjarc );

    jarc = new Arc( Arc.SIDE_RIGHT, 0 );
    arctessellator.bezier( jarc, smax, smax, tmin, tmax );
    initialbin.addarc( jarc  );
    pjarc = jarc.append( pjarc );

    jarc = new Arc( Arc.SIDE_TOP, 0 );
    arctessellator.bezier( jarc, smax, smin, tmax, tmax );
    initialbin.addarc( jarc  );
    pjarc = jarc.append( pjarc );

    jarc = new Arc( Arc.SIDE_LEFT, 0 );
    arctessellator.bezier( jarc, smin, smin, tmax, tmin );
    initialbin.addarc( jarc  );
    jarc.append( pjarc );

    assert( jarc.check() );
  }

  private void split( Bin &, int, const float *, int, int );
  private void partition( Bin bin, Bin left, Bin intersections, Bin right, Bin unknown, int param, float value ) {
    Bin  headonleft = new Bin();
    Bin headonright = new Bin();
    Bin tailonleft = new Bin();
    Bin tailonright = new Bin();

    for( Arc jarc = bin.removearc(); jarc != null; jarc = bin.removearc() ) {

      float tdiff = jarc.tail()[param] - value;
      float hdiff = jarc.head()[param] - value;

      if( tdiff > 0.0 ) {
        if( hdiff > 0.0 ) {
          right.addarc( jarc  );
        } else if( hdiff == 0.0 ) {
          tailonright.addarc( jarc  );
        } else {
          Arc jtemp;
          switch( arc_split(jarc, param, value, 0) ) {
          case 2:
            tailonright.addarc( jarc  );
            headonleft.addarc( jarc.next  );
            break;
          case 31:
            assert( jarc.head()[param] > value );
            right.addarc( jarc  );
            tailonright.addarc( jtemp = jarc.next  );
            headonleft.addarc( jtemp.next  );
            break;
          case 32:
            assert( jarc.head()[param] <= value );
            tailonright .addarc( jarc  );
            headonleft.addarc( jtemp = jarc.next  );
            left.addarc( jtemp.next  );
            break;
          case 4:
            right.addarc( jarc  );
            tailonright.addarc( jtemp = jarc.next  );
            headonleft.addarc( jtemp = jtemp.next  );
            left.addarc( jtemp.next  );
          }
        }
      } else if( tdiff == 0.0 ) {
        if( hdiff > 0.0 ) {
          headonright.addarc( jarc  );
        } else if( hdiff == 0.0 ) {
          unknown.addarc( jarc  );
        } else {
          headonleft.addarc( jarc  );
        }
      } else {
        if( hdiff > 0.0 ) {
          Arc jtemp;
          switch( arc_split(jarc, param, value, 1) ) {
          case 2:
            tailonleft.addarc( jarc  );
            headonright.addarc( jarc.next  );
            break;
          case 31:
            assert( jarc.head()[param] < value );
            left.addarc( jarc  );
            tailonleft.addarc( jtemp = jarc.next  );
            headonright.addarc( jtemp.next  );
            break;
          case 32:
            assert( jarc.head()[param] >= value );
            tailonleft.addarc( jarc  );
            headonright.addarc( jtemp = jarc.next  );
            right.addarc( jtemp.next  );
            break;
          case 4:
            left.addarc( jarc  );
            tailonleft.addarc( jtemp = jarc.next  );
            headonright.addarc( jtemp = jtemp.next  );
            right.addarc( jtemp.next  );
          }
        } else if( hdiff == 0.0 ) {
          tailonleft.addarc( jarc  );
        } else {
          left.addarc( jarc  );
        }
      }
    }
    if( param == 0 ) {
      classify_headonleft_s( headonleft, intersections, left, value );
      classify_tailonleft_s( tailonleft, intersections, left, value );
      classify_headonright_s( headonright, intersections, right, value );
      classify_tailonright_s( tailonright, intersections, right, value );
    } else {
      classify_headonleft_t( headonleft, intersections, left, value );
      classify_tailonleft_t( tailonleft, intersections, left, value );
      classify_headonright_t( headonright, intersections, right, value );
      classify_tailonright_t( tailonright, intersections, right, value );
    }
  }

  /**
   * Determine points of non-monotonicity in s direction.
   */
  private void findIrregularS( Bin bin ) {
    assert( bin.firstarc() == null || bin.firstarc().check() );

    smbrkpts.grow( bin.numarcs() );

    for( Arc jarc=bin.firstarc(); jarc != null; jarc=bin.nextarc() ) {
      float[] a = jarc.prev.tail();
      float[] b = jarc.tail();
      float[] c = jarc.head();

      if( b[1] == a[1] && b[1] == c[1] ) continue;

      //corrected code
      if((b[1]<=a[1] && b[1] <= c[1]) ||
         (b[1]>=a[1] && b[1] >= c[1]))
        {
          //each arc (jarc, jarc.prev, jarc.next) is a 
          //monotone arc consisting of multiple line segements.
          //it may happen that jarc.prev and jarc.next are the same,
          //that is, jarc.prev and jarc form a closed loop.
          //In such case, a and c will be the same.
          if(a[0]==c[0] && a[1] == c[1])
            {
              if(jarc.pwlArc.npts >2)
                {
                  c = jarc.pwlArc.pts[jarc.pwlArc.npts-2].param;
                }
              else
                {
                  assert(jarc.prev.pwlArc.npts>2);
                  a = jarc.prev.pwlArc.pts[jarc.prev.pwlArc.npts-2].param;
                }
        
            }
          if(area(a,b,c) < 0)
            {
              smbrkpts.add(b[0]);
            }

        }

      /* old code, 
         if( b[1] <= a[1] && b[1] <= c[1] ) {
         if( ! ccwTurn_tr( jarc.prev, jarc ) )
         smbrkpts.add( b[0] );
         } else if( b[1] >= a[1] && b[1] >= c[1] ) {
         if( ! ccwTurn_tl( jarc.prev, jarc ) )
         smbrkpts.add( b[0] );
         }
      */

    }

    smbrkpts.filter();
  }

  /**
   * Determines points of non-monotonicity in t direction where one
   * arc is parallel to the s axis.
   */
  private void findIrregularT( Bin bin ) {
    assert( bin.firstarc() == null || bin.firstarc().check() );

    tmbrkpts.grow( bin.numarcs() );

    for( Arc jarc=bin.firstarc(); jarc != null; jarc=bin.nextarc() ) {
      float[] a = jarc.prev.tail();
      float[] b = jarc.tail();
      float[] c = jarc.head();

      if( b[0] == a[0] && b[0] == c[0] ) continue;

      if( b[0] <= a[0] && b[0] <= c[0] ) {
        if( a[1] != b[1] && b[1] != c[1] ) continue; 
        if( ccwTurn_sr( jarc.prev, jarc ) == 0)
          tmbrkpts.add( b[1] );
      } else if ( b[0] >= a[0] && b[0] >= c[0] ) {
        if( a[1] != b[1] && b[1] != c[1] ) continue; 
        if( ccwTurn_sl( jarc.prev, jarc ) == 0)
          tmbrkpts.add( b[1] );
      }
    }
    tmbrkpts.filter( );
  }


  private int bbox( TrimVertex a, TrimVertex b, TrimVertex c, int p ) {
    return bbox( a.param[p], b.param[p], c.param[p],
           a.param[1-p], b.param[1-p], c.param[1-p] );
  }

  private static int bbox( float sa, float sb, float sc, float ta, float tb, float tc ) {
    assert( tc >= ta );
    assert( tc <= tb );

    if( sa < sb ) {
      if( sc <= sa ) {
        return -1;
      } else if( sb <= sc ) {
        return 1;
      } else {
        return 0;
      }
    } else if( sa > sb ) {
      if( sc >= sa ) {
        return 1;
      } else if( sb >= sc ) {
        return -1;
      } else {
        return 0;
      }
    } else {
      if( sc > sa ) {
        return 1;
      } else if( sb > sc ) {
        return -1;
      } else {
        return 0;
      }
    }
  }
  /**
   * Determines how three points are oriented by computing their
   * determinant.
   *
   * @return 1 if the vertices are ccw oriented, 0 if they are cw
   * oriented, or -1 if the computation is ill-conditioned.
   */
  private static int ccw( TrimVertex a, TrimVertex b, TrimVertex c ) {
    float d = TrimVertex.det3( a, b, c );
    if( Math.abs(d) < 0.0001 ) return -1;
    return (d < 0.0) ? 0 : 1;
  }
  private void join_s( Bin &, Bin &, Arc_ptr, Arc_ptr  );
  private void join_t( Bin &, Bin &, Arc_ptr , Arc_ptr  );

  private static void vert_interp( TrimVertex n, TrimVertex l, TrimVertex r, int p, float val ) {
    assert( val > l.param[p]);
    assert( val < r.param[p]);

    n.nuid = l.nuid;

    n.param[p] = val;
    if( l.param[1-p] != r.param[1-p]  ) {
      float ratio = (val - l.param[p]) / (r.param[p] - l.param[p]);
      n.param[1-p] = l.param[1-p] + 
        ratio * (r.param[1-p] - l.param[1-p]);
    } else {
      n.param[1-p] = l.param[1-p];
    }
  }

  private static final int INTERSECT_VERTEX = 1;
  private static final int INTERSECT_EDGE   = 2;

  /**
   * Finds intersection of pwlArc and isoparametric line.
   */
  private static int pwlarc_intersect( PwlArc pwlArc, int param, float value, int dir, int[] loc ) {
    assert( pwlArc.npts > 0 );

    if( dir != 0 ) {
      TrimVertex[] v = pwlArc.pts;
      int imin = 0; 
      int imax = pwlArc.npts - 1;
      assert( value > v[imin].param[param] );
      assert( value < v[imax].param[param] );	
      while( (imax - imin) > 1 ) {
        int imid = (imax + imin)/2;
        if( v[imid].param[param] > value )
          imax = imid;
        else if( v[imid].param[param] < value )
          imin = imid;
        else {
          loc[1] = imid;
          return INTERSECT_VERTEX;
        }
      }
      loc[0] = imin;
      loc[2] = imax;
      return INTERSECT_EDGE;
    } else {
      TrimVertex[] v = pwlArc.pts;
      int imax = 0; 
      int imin = pwlArc.npts - 1;
      assert( value > v[imin].param[param] );
      assert( value < v[imax].param[param] );	
      while( (imin - imax) > 1 ) {
        int imid = (imax + imin)/2;
        if( v[imid].param[param] > value )
          imax = imid;
        else if( v[imid].param[param] < value )
          imin = imid;
        else {
          loc[1] = imid;
          return INTERSECT_VERTEX;
        }
      }
      loc[0] = imin;
      loc[2] = imax;
      return INTERSECT_EDGE;
    }
  }

  private int arc_split( Arc jarc , int param, float value, int dir ) {
    int maxvertex = jarc.pwlArc.npts;
    Arc jarc1, jarc2, jarc3;
    TrimVertex v = jarc.pwlArc.pts;

    int[] loc = new int[3];
    switch( pwlarc_intersect( jarc.pwlArc, param, value, dir, loc ) ) {

      // When the parameter value lands on a vertex, life is sweet
    case INTERSECT_VERTEX: {
      jarc1 = new Arc( jarc, new PwlArc( maxvertex-loc[1], /* &v[loc[1]] */ v, loc[1] ) );
      jarc.pwlArc.npts = loc[1] + 1;
      jarc1.next = jarc.next;
      jarc1.next.prev = jarc1;
      jarc.next = jarc1;
      jarc1.prev = jarc;
      assert(jarc.check());
      return 2;
    }

      // When the parameter value intersects an edge, we have to
      // interpolate a new vertex.  There are special cases
      // if the new vertex is adjacent to one or both of the
      // endpoints of the arc.
    case INTERSECT_EDGE: {
      int i, j;
      if( dir == 0 ) {
        i = loc[0];
        j = loc[2];
      } else {
        i = loc[2];
        j = loc[0];
      }

      // The split is between vertices at index j and i, in that
      // order (j < i)
      
      // JEB:  This code is my idea of how to do the split without
      // increasing the number of links.  I'm doing this so that
      // the is_rect routine can recognize rectangles created by
      // subdivision.  In exchange for simplifying the curve list,
      // however, it costs in allocated space and vertex copies.
      
      TrimVertex[] newjunk = TrimVertex.allocate(maxvertex -i+1 /*-j*/);
      int k;
      for(k=0; k<maxvertex-i; k++)
        {
          newjunk[k+1] = v[i+k];
          newjunk[k+1].nuid = jarc.nuid;
        }
      
      TrimVertex[] vcopy = TrimVertex.allocate(maxvertex);
      for(k=0; k<maxvertex; k++)
        {
          vcopy[k].param[0] = v[k].param[0];
          vcopy[k].param[1] = v[k].param[1];
        }
      jarc.pwlArc.pts=vcopy;

      v[i].nuid = jarc.nuid;
      v[j].nuid = jarc.nuid;
      vert_interp( newjunk[0], v[loc[0]], v[loc[2]], param, value );

      if( showingDegenerate() )
        backend.triangle( v[i], newjunk[0], v[j] );

      vcopy[j+1].param[0]=newjunk[0].param[0];
      vcopy[j+1].param[1]=newjunk[0].param[1];


      jarc1 = new Arc( jarc,
                       new PwlArc(maxvertex-i+1 , newjunk ) );

      jarc.pwlArc.npts = j+2;
      jarc1.next = jarc.next;
      jarc1.next.prev = jarc1;
      jarc.next = jarc1;
      jarc1.prev = jarc;
      assert(jarc.check());

      return 2;

      /***
      // JEB: This is the original version:

      TrimVertex[] newjunk = TrimVertex.allocate(3);
      v[i].nuid = jarc.nuid;
      v[j].nuid = jarc.nuid;
      newjunk[0] = v[j];
      newjunk[2] = v[i];
      vert_interp( &newjunk[1], &v[loc[0]], &v[loc[2]], param, value );

      if( showingDegenerate() )
        backend.triangle( &newjunk[2], &newjunk[1], &newjunk[0] );

      // New vertex adjacent to both endpoints
      if (maxvertex == 2) {
        jarc1 = new(arcpool) Arc( jarc, new(pwlarcpool) PwlArc( 2, newjunk+1 ) );
        jarc.pwlArc.npts = 2;
        jarc.pwlArc.pts = newjunk;
        jarc1.next = jarc.next;
        jarc1.next.prev = jarc1;
        jarc.next = jarc1;
        jarc1.prev = jarc;
        assert(jarc.check() != 0);

        return 2;

        // New vertex adjacent to ending point of arc
      } else if (maxvertex - j == 2) {
        jarc1 = new(arcpool) Arc( jarc, new(pwlarcpool) PwlArc( 2, newjunk ) );
        jarc2 = new(arcpool) Arc( jarc, new(pwlarcpool) PwlArc( 2, newjunk+1 ) );
        jarc.pwlArc.npts = maxvertex-1;
        jarc2.next = jarc.next;
        jarc2.next.prev = jarc2;
        jarc.next = jarc1;
        jarc1.prev = jarc;
        jarc1.next = jarc2;
        jarc2.prev = jarc1;
        assert(jarc.check() != 0);
        return 31;

        // New vertex adjacent to starting point of arc
      } else if (i == 1) {
        jarc1 = new(arcpool) Arc( jarc, new(pwlarcpool) PwlArc( 2, newjunk+1 ) );
        jarc2 = new(arcpool) Arc( jarc, 
                                  new(pwlarcpool) PwlArc( maxvertex-1, &jarc.pwlArc.pts[1] ) );
        jarc.pwlArc.npts = 2;
        jarc.pwlArc.pts = newjunk;
        jarc2.next = jarc.next;
        jarc2.next.prev = jarc2;
        jarc.next = jarc1;
        jarc1.prev = jarc;
        jarc1.next = jarc2;
        jarc2.prev = jarc1;
        assert(jarc.check() != 0);
        return 32;

        // It's somewhere in the middle
      } else {
        jarc1 = new(arcpool) Arc( jarc, new(pwlarcpool) PwlArc( 2, newjunk ) );
        jarc2 = new(arcpool) Arc( jarc, new(pwlarcpool) PwlArc( 2, newjunk+1 ) );
        jarc3 = new(arcpool) Arc( jarc, new(pwlarcpool) PwlArc( maxvertex-i, v+i ) );
        jarc.pwlArc.npts = j + 1;
        jarc3.next = jarc.next;
        jarc3.next.prev = jarc3;
        jarc.next = jarc1;
        jarc1.prev = jarc;
        jarc1.next = jarc2;
        jarc2.prev = jarc1;
        jarc2.next = jarc3;
        jarc3.prev = jarc2;
        assert(jarc.check() != 0);
        return 4;
      }
      ***/
    }
    default:
      return -1; //picked -1 since it's not used
    }
  }

  private void check_s( Arc_ptr , Arc_ptr  );
  private void check_t( Arc_ptr , Arc_ptr  );
  private inline void link( Arc_ptr , Arc_ptr , Arc_ptr , Arc_ptr  );
  private inline void simple_link( Arc_ptr , Arc_ptr  );

  private Bin makePatchBoundary( const float[] from, const float[] to ) {
    Bin ret = new Bin();
    float smin = from[0];
    float smax = to[0];
    float tmin = from[1];
    float tmax = to[1];

    pjarc = 0;

    Arc jarc = new Arc( arc_bottom, 0 );
    arctessellator.bezier( jarc, smin, smax, tmin, tmin );
    ret.addarc( jarc  );
    pjarc = jarc.append( pjarc );

    jarc = new(arcpool) Arc( arc_right, 0 );
    arctessellator.bezier( jarc, smax, smax, tmin, tmax );
    ret.addarc( jarc  );
    pjarc = jarc.append( pjarc );

    jarc = new(arcpool) Arc( arc_top, 0 );
    arctessellator.bezier( jarc, smax, smin, tmax, tmax );
    ret.addarc( jarc  );
    pjarc = jarc.append( pjarc );

    jarc = new(arcpool) Arc( arc_left, 0 );
    arctessellator.bezier( jarc, smin, smin, tmax, tmin );
    ret.addarc( jarc  );
    jarc.append( pjarc );

    assert( jarc.check() != 0 );
    return ret;
  }

   /*in domain distance method, the tessellation is controled by two numbers:
    *GLU_U_STEP: number of u-segments per unit u length of domain
    *GLU_V_STEP: number of v-segments per unit v length of domain
    *These two numbers are normally stored in mapdesc.maxs(t)rate.
    *I (ZL) put these two numbers here so that I can optimize the untrimmed 
    *case in the case of domain distance sampling.
    *These two numbers are set by set_domain_distance_u_rate() and ..._v_..().
    */
  private float domain_distance_u_rate;
  private float domain_distance_v_rate;
  private int is_domain_distance_sampling;
  
}
