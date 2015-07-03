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
 * Class holding list of Mapdescs
 * @author Tomáš Hráský
 *
 */
public class Maplist {
  /**
   * Head of linked list
   */
  private Mapdesc maps;

  /**
   * Backend class
   * private final Backend backend;
   */

  /**
   * Makes new Maplist
   */
  public Maplist(/* final Backend backend */) {
    // this.backend = backend;
  }

  /**
   * Sets linked list beginning to null
   */
  public void initialize() {
    // TODO mapdespool.clear ?
    maps = null;
  }

  /**
   * Defines new Mapdesc if it is not defined and appends it to linked list
   * @param type map type
   * @param rational is map rational
   * @param ncoords number of coords
   */
  public void define(final int type, final int rational, final int ncoords) {
    // DONE
    final Mapdesc m = locate(type);
    assert (m == null || (m.isrational == rational && m.ncoords == ncoords));
    add(type, rational, ncoords);

  }

  /**
   * Adds new Mapdesc to linked list
   * @param type map type
   * @param rational is map rational
   * @param ncoords number of coords
   */
  private void add(final int type, final int rational, final int ncoords) {
    // DONE
    final Mapdesc map = new Mapdesc(type, rational, ncoords);
    if (maps == null) {
      maps = map;
    } else {
      map.next = maps;
      maps = map;
    }
  }

  /**
   * Tries to find Mapdesc in linked list
   * @param type map type
   * @return Mapdesc of type or null if there is no such map
   */
  public Mapdesc locate(final int type) {
    // DONE
    Mapdesc m = null;
    for (m = maps; m != null; m = m.next)
      if (m.getType() == type)
        break;
    return m;
  }

  /**
   * Alias for locate
   * @param type maptype
   * @return Mapdesc of type or null if there is no such map
   */
  public Mapdesc find(final int type) {
    return locate(type);
  }
}
