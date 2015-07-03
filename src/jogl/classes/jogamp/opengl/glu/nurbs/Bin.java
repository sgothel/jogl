package jogamp.opengl.glu.nurbs;

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

/**
 * Class holding trimming arcs
 * @author Tomas Hrasky
 *
 */
public class Bin {

  /**
   * Head of linked list of arcs
   */
  private Arc head;

  /**
   * Current arc
   */
  private Arc current;

  /**
   * Indicates whether there are any Arcs in linked list
   * @return true if there are any Arcs in linked list
   */
  public boolean isnonempty() {
    // DONE
    return this.head != null ? true : false;
  }

  /**
   * Adds and arc to linked list
   * @param jarc added arc
   */
  public void addarc(final Arc jarc) {
    // DONE
    // if (head == null)
    // head = jarc;
    // else {
    jarc.link = head;
    head = jarc;
    // }

  }

  /**
   * Returns number of arcs in linked list
   * @return number of arcs
   */
  public int numarcs() {
    // DONE
    int count = 0;
    for (Arc jarc = firstarc(); jarc != null; jarc = nextarc())
      count++;
    return count;
  }

  /**
   * Removes first arc in list
   * @return new linked list head
   */
  public Arc removearc() {
    // DONE
    final Arc jarc = head;
    if (jarc != null)
      head = jarc.link;
    return jarc;

  }

  /**
   * Consolidates linked list
   */
  public void adopt() {
    // DONE
    markall();

    Arc orphan;
    while ((orphan = removearc()) != null) {
      for (Arc parent = orphan.next; !parent.equals(orphan); parent = parent.next) {
        if (!parent.ismarked()) {
          orphan.link = parent.link;
          parent.link = orphan;
          orphan.clearmark();
          break;
        }
      }
    }

  }

  /**
   * Marks all arc in linked list
   */
  private void markall() {
    // DONE
    for (Arc jarc = firstarc(); jarc != null; jarc = nextarc())
      jarc.setmark();
  }

  /**
   * Returns first arc in linked list
   * @return first arc in linked list
   */
  private Arc firstarc() {
    // DONE
    current = head;
    return nextarc();
  }

  /**
   * Returns next arc in linked list
   * @return next arc
   *
   */
  private Arc nextarc() {
    // DONE
    final Arc jarc = current;
    if (jarc != null)
      current = jarc.link;
    return jarc;
  }
}
