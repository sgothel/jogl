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

/* an arc, in two list, the trim list and bin */
public class Arc {
  public static final int SIDE_NONE   = 0;
  public static final int SIDE_RIGHT  = 1;
  public static final int SIDE_TOP    = 2;
  public static final int SIDE_LEFT   = 3;
  public static final int SIDE_BOTTOM = 4;

  public static final int bezier_tag = (1 << 13);
  public static final int arc_tag    = (1 <<  3);
  public static final int tail_tag   = (1 <<  6);
  public Arc    prev;  /* trim list pointer */
  public Arc    next;  /* trim list pointer */
  public Arc    link;  /* bin pointers */
  public BezierArc bezierArc; /* associated bezier arc */
  public PwlArc    pwlArc;    /* associated pwl arc */
  public long      type;      /* curve type */
  public long      nuid;

  private static final float ZERO = 0.00001f;

  public Arc(Arc j, PwlArc p) {
    pwlArc = p;
    type = j.type;
    nuid = j.nuid;
  }

  public Arc(int arcSide, long nuid) {
    type = 0;
    setside(arcSide);
    this.nuid = nuid;
  }

  public Arc append(Arc jarc) {
    if ( jarc != null ) {
      next = jarc.next;
      prev = jarc;
      next.prev = prev.next = this;
    } else {
      next = prev = this;
    }
    return this;
  }

  public boolean check() {
    Arc jarc = this;
    do {
      assert( (jarc.pwlArc != null) || (jarc.bezierArc != null) );

      if (jarc.prev == 0 || jarc.next == 0) {
        System.out.println( "checkjarc:null next/prev pointer");
        jarc.print( );
        return false;
      }

      if (jarc.next.prev != jarc) {
        System.out.println( "checkjarc: pointer linkage screwed up");
        jarc.print( );
        return false;
      }

      if( jarc.pwlArc != null ) {
        assert( jarc.pwlArc.npts >= 1 );
        assert( jarc.pwlArc.npts < 100000 );
        if( jarc.prev.pwlArc != null ) {
          if( jarc.tail()[1] != jarc.prev.rhead()[1] ) {
            System.out.println( "checkjarc: geometric linkage screwed up 1");
            jarc.prev.show();
            jarc.show();
            return false;
          }
          if( jarc.tail()[0] != jarc.prev.rhead()[0] ) {
            System.out.println( "checkjarc: geometric linkage screwed up 2");
            jarc.prev.show();
            jarc.show();
            return false;
          }
        }
        if( jarc.next.pwlArc ) {
          if( jarc.next.tail()[0] != jarc.rhead()[0] ) {
            System.out.println( "checkjarc: geometric linkage screwed up 3");
            jarc.show();
            jarc.next.show();
            return false;
          }
          if( jarc.next.tail()[1] != jarc.rhead()[1] ) {
            System.out.println( "checkjarc: geometric linkage screwed up 4");
            jarc.show();
            jarc.next.show();
            return false;
          }
        }
        if( jarc.isbezier() ) {
          assert( jarc.pwlArc.npts == 2 );
          assert( (jarc.pwlArc.pts[0].param[0] ==
                   jarc.pwlArc.pts[1].param[0]) ||
                  (jarc.pwlArc.pts[0].param[1] ==
                   jarc.pwlArc.pts[1].param[1]) );
        }
      }
      jarc = jarc.next;
    } while (jarc != this);
    return true;
  }

  /**
   * Checks if tail of arc and head of prev meet.
   */
  public boolean isDisconnected() {
    if( pwlArc == 0 ) return 0;
    if( prev.pwlArc == 0 ) return 0;

    float[] p0 = tail();
    float[] p1 = prev.rhead();

    if( ((p0[0] - p1[0]) > ZERO) || ((p1[0] - p0[0]) > ZERO) ||
	((p0[1] - p1[1]) > ZERO) || ((p1[1] - p0[1]) > ZERO)  ) {
      return true;
    } else {
      /* average two points together */
      p0[0] = p1[0] = (p1[0] + p0[0]) * 0.5f;
      p0[1] = p1[1] = (p1[1] + p0[1]) * 0.5f;
      return false;
    }
  }

  /**
   * Counts number of points on arc loop.
   */
  public int numpts( ) {
    Arc jarc = this;
    int npts = 0;
    do {
	npts += jarc.pwlArc.npts;
	jarc = jarc.next;
    } while( jarc != this );
    return npts;
  }

  /**
   * Marks each point with id of arc.
   */
  public void markverts( void ) {
    Arc jarc = this;
	
    do {
	TrimVertex p = jarc.pwlArc.pts;
	for( int i=0; i<jarc.pwlArc.npts; i++ )
	    p[i].nuid = jarc.nuid;
	jarc = jarc.next;
    } while( jarc != this );
  }

  /**
   * Finds axis extrema on arc loop.
   */
  public void    getextrema( Arc[4] ) {
    float leftpt, botpt, rightpt, toppt;

    extrema[0] = extrema[1] = extrema[2] = extrema[3] = this;

    leftpt = rightpt = this.tail()[0];
    botpt  = toppt   = this.tail()[1];

    for( Arc jarc = this.next; jarc != this; jarc = jarc.next ) {
	if ( jarc.tail()[0] <  leftpt || 
	    (jarc.tail()[0] <= leftpt && jarc.rhead()[0]<=leftpt))  {
	    leftpt = jarc.pwlArc.pts.param[0];
	    extrema[1] = jarc;
	}
	if ( jarc.tail()[0] >  rightpt || 
	    (jarc.tail()[0] >= rightpt && jarc.rhead()[0] >= rightpt)) {
	    rightpt = jarc.pwlArc.pts.param[0];
	    extrema[3] = jarc;
	}
	if ( jarc.tail()[1] <  botpt || 
            (jarc.tail()[1] <= botpt && jarc.rhead()[1] <= botpt ))  {
	    botpt = jarc.pwlArc.pts.param[1];
	    extrema[2] = jarc;
	}
	if ( jarc.tail()[1] >  toppt || 
	    (jarc.tail()[1] >= toppt && jarc.rhead()[1] >= toppt))  {
	    toppt = jarc.pwlArc.pts.param[1];
	    extrema[0] = jarc;
	}
    }
  }

  /**
   * Prints out the vertices of all pwl arcs on a loop.
   */
  public void    print( ) {
    Arc jarc = this;

    do {
      jarc.show( );
      jarc = jarc.next;
    } while (jarc != this);
  }

  public void    show( ) {
    System.out.println( "\tPWLARC NP: " + pwlArc.npts + " FL: 1");
    for( int i = 0; i < pwlArc.npts; i++ ) {
      System.out.println( "\t\tVERTEX " + pwlArc.pts[i].param[0] + " " +
                          pwlArc.pts[i].param[1] );
    }
  }

  /**
   * Attaches a pwl arc to an arc and mark it as a border arc.
   */
  public void    makeSide( PwlArc pwl, int arcSide ) {
    assert( pwl != 0);
    assert( pwlArc == 0 );
    assert( pwl.npts > 0 );
    assert( pwl.pts != 0);
    pwlArc = pwl;
    clearbezier();
    setside( arcSide );
  }

  public boolean      isTessellated()        { return (pwlArc != null); }
  public boolean      isbezier()             { return (type & bezier_tag) != 0; }
  public void         setbezier()            { type |= bezier_tag; }
  public void         clearbezier()          { type &= ~bezier_tag; }
  public long         npts()                 { return pwlArc.npts; }
  public TrimVertex[] pts()                  { return pwlArc.pts; }
  public float[]      tail()                 { return pwlArc.pts[0].param; }
  public float[]      head()                 { return next.pwlArc.pts[0].param; }
  public float[]      rhead()                { return pwlArc.pts[pwlArc.npts-1].param; }
  public long         ismarked()             { return type & arc_tag; }
  public void         setmark()              { type |= arc_tag; }
  public void         clearmark()            { type &= (~arc_tag); }
  public void         clearside()            { type &= ~(0x7 << 8); }
  public void         setside( int arcSide ) { clearside(); type |= (((long)arcSide)<<8); }
  public int          getside()              { return ((type>>8) & 0x7); }
  public int          getitail()             { return type & tail_tag; }
  public void         setitail()             { type |= tail_tag; }
  public void         clearitail()           { type &= (~tail_tag); }
}
