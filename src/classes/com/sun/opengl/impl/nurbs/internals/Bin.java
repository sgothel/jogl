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

public class Bin {
  private Arc head;    /*first arc on list */
  private Arc current; /* current arc on list */

  /**
   * Sets current arc to first arc of bin; advances to next arc.
   */
  public Arc firstarc( ) {
    current = head;
    return nextarc( );
  }
  /**
   * Returns current arc in bin and advances pointer to next arc.
   */
  public Arc nextarc( ) {
    Arc jarc = current;

    assert( (jarc == null) || jarc.check() );

    if( jarc != null ) current = jarc.link;
    return jarc;
  }
  /**
   * Removes first Arc from bin.
   */
  public Arc removearc( ) {
    Arc jarc = head;

    if( jarc != null ) head = jarc.link;
    return jarc;
  }
  public boolean isnonempty( ) { return (head != null); }
  /**
   * Adds an Arc to head of the linked list of Arcs.
   */
  public void addarc( Arc jarc ) {
    jarc.link = head;
    head = jarc;
  }
  /**
   * Removes given Arc from bin.
   */
  public void  remove_this_arc( Arc arc ) {
    Arc j, prev;
    for( j = head; (j != null) && (j != arc); prev = j, j = j.link );

    if( j != null ) {
      if( j == current )
        current = j.link;
      if ( prev != null )
        prev.link = j.link;
    }
  }
  /**
   * Counts number of arcs in bin.
   */
  public int numarcs( ) {
    long count = 0;
    for( Arc jarc = firstarc(); jarc != null; jarc = nextarc() )
	count++;
    return count;
  }
  /**
   * Places an orphaned arc into its new parent's bin.
   */
  public void adopt( ) {
    markall();

    Arc orphan;
    while( (orphan = removearc()) != null ) {
      for( Arc parent = orphan.next; parent != orphan; parent = parent.next ) {
        if (! parent.ismarked() ) {
          orphan.link = parent.link;
          parent.link = orphan;
          orphan.clearmark();
          break;
        }
      }
    }
  }
  /**
   * Marks all arcs with an identifying tag.
   */
  public void markall( ) {
    for( Arc jarc = firstarc(); jarc != null; jarc = nextarc() )
      jarc.setmark();
  }
  /**
   * Prints out descriptions of the arcs in the bin.
   */
  public void show( String name ) {
    System.out.println( name );
    for( Arc jarc = firstarc(); jarc != null; jarc = nextarc() )
        jarc.show( );
  }
  /**
   * Prints out all arcs that are untessellated border arcs.
   */
  public void listBezier( ) {
    for( Arc jarc = firstarc(); jarc != null; jarc = nextarc() ) {
      if( jarc.isbezier( ) ) {
        assert( jarc.pwlArc.npts == 2 );	
        TrimVertex[] pts = jarc.pwlArc.pts;
        float s1 = pts[0].param[0];
        float t1 = pts[0].param[1];
        float s2 = pts[1].param[0];
        float t2 = pts[1].param[1];
        System.out.println( "arc ( " + s1 + "," + t1 + ") (" +
                            s2 + "," + t2 + ")");
      }
    }
  }
}
