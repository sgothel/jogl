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
 * NURBS definition
 * @author Tomas Hrasky
 *
 */
public class Splinespec {

  /**
   * Dimension
   */
  private final int dim;

  /**
   * Knot vector specs
   */
  private Knotspec kspec;

  /**
   * Control points after conversion
   */
  private CArrayOfFloats outcpts;

  /**
   * Makes new Splinespec with given dimension
   * @param i dimension
   */
  public Splinespec(final int i) {
    // DONE
    this.dim = i;
  }

  /**
   * Initializes knotspec according to knotvector
   * @param knotvector basic knotvector
   */
  public void kspecinit(final Knotvector knotvector) {
    // DONE
    this.kspec = new Knotspec();
    kspec.inkbegin = new CArrayOfFloats(knotvector.knotlist, 0);
    kspec.inkend = new CArrayOfFloats(knotvector.knotlist,
                                      knotvector.knotcount);
    kspec.prestride = knotvector.stride;
    kspec.order = knotvector.order;
    kspec.next = null;
  }

  /**
   * Initializes knotspec according to knotvector - SURFACE
   * @param sknotvector knotvector in u dir
   * @param tknotvector knotvector in v dir
   */
  public void kspecinit(final Knotvector sknotvector, final Knotvector tknotvector) {
    // DONE
    this.kspec = new Knotspec();
    final Knotspec tkspec = new Knotspec();

    kspec.inkbegin = new CArrayOfFloats(sknotvector.knotlist, 0);
    kspec.inkend = new CArrayOfFloats(sknotvector.knotlist,
                                      sknotvector.knotcount);
    kspec.prestride = sknotvector.stride;
    kspec.order = sknotvector.order;
    kspec.next = tkspec;

    tkspec.inkbegin = new CArrayOfFloats(tknotvector.knotlist, 0);
    tkspec.inkend = new CArrayOfFloats(tknotvector.knotlist,
                                       tknotvector.knotcount);
    tkspec.prestride = tknotvector.stride;
    tkspec.order = tknotvector.order;
    tkspec.next = null;
  }

  /**
   * Preselect and select knotspecs
   */
  public void select() {
    // DONE
    for (Knotspec knotspec = kspec; knotspec != null; knotspec = knotspec.next) {
      knotspec.preselect();
      knotspec.select();
    }

  }

  /**
   * Prepares for conversion
   * @param ncoords number of coords
   */
  public void layout(final int ncoords) {
    // DONE
    int stride = ncoords;
    for (Knotspec knotspec = kspec; knotspec != null; knotspec = knotspec.next) {
      knotspec.poststride = stride;
      stride *= (knotspec.bend.getPointer() - knotspec.bbegin
                 .getPointer())
        * knotspec.order + knotspec.postoffset;
      knotspec.preoffset *= knotspec.prestride;
      knotspec.prewidth *= knotspec.poststride;
      knotspec.postwidth *= knotspec.poststride;
      knotspec.postoffset *= knotspec.poststride;
      knotspec.ncoords = ncoords;
    }
    outcpts = new CArrayOfFloats(new float[stride]);

  }

  /**
   * Prepares quilt for conversion
   * @param quilt quilt to work with
   */
  public void setupquilt(final Quilt quilt) {
    // DONE
    final CArrayOfQuiltspecs qspec = new CArrayOfQuiltspecs(quilt.qspec);
    quilt.eqspec = new CArrayOfQuiltspecs(qspec.getArray(), dim);
    for (Knotspec knotspec = kspec; knotspec != null;) {
      qspec.get().stride = knotspec.poststride;
      qspec.get().width = knotspec.bend.getPointer()
        - knotspec.bbegin.getPointer();
      qspec.get().order = knotspec.order;
      qspec.get().offset = knotspec.postoffset;
      qspec.get().index = 0;
      qspec.get().bdry[0] = (knotspec.kleft.getPointer() == knotspec.kfirst
                             .getPointer()) ? 1 : 0;
      qspec.get().bdry[1] = (knotspec.kright.getPointer() == knotspec.klast
                             .getPointer()) ? 1 : 0;
      qspec.get().breakpoints = new float[qspec.get().width + 1];
      final CArrayOfFloats k = new CArrayOfFloats(qspec.get().breakpoints, 0);
      for (final CArrayOfBreakpts bk = new CArrayOfBreakpts(knotspec.bbegin); bk
             .getPointer() <= knotspec.bend.getPointer(); bk.pp()) {
        k.set(bk.get().value);
        k.pp();
      }
      knotspec = knotspec.next;
      if (knotspec != null)
        qspec.pp();
    }
    quilt.cpts = new CArrayOfFloats(outcpts);
    quilt.next = null;
  }

  /**
   * Copies array of control points to output array
   * @param ctlarray control points array
   */
  public void copy(final CArrayOfFloats ctlarray) {
    // DONE
    kspec.copy(ctlarray, outcpts);

  }

  /**
   * Transforms knotspecs - conversion
   */
  public void transform() {
    // DONE
    Knotspec knotspec;
    outcpts.setPointer(0);
    for (knotspec = kspec; knotspec != null; knotspec = knotspec.next)
      knotspec.istransformed = false;

    for (knotspec = kspec; knotspec != null; knotspec = knotspec.next) {
      for (Knotspec kspec2 = kspec; kspec2 != null; kspec2 = kspec2.next)
        kspec2.kspectotrans = knotspec;
      kspec.transform(outcpts);
      knotspec.istransformed = true;
    }

  }
}
