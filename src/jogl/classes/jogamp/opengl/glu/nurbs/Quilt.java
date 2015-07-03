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
 * Class for converting NURBS curves and surfaces to list of bezier arcs or patches repectively
 * @author Tomáš Hráský
 *
 */
public class Quilt {
  /**
   * Maximum quilt dimension
   */
  private static final int MAXDIM = 2;

  /**
   * List of map descriptions
   */
  Mapdesc mapdesc;

  /**
   *  Array of quiltspecs pointer
   */
  public CArrayOfQuiltspecs qspec;

  /**
   * End array of quilt specs pointer
   */
  public CArrayOfQuiltspecs eqspec;

  /**
   * Control points
   */
  public CArrayOfFloats cpts;

  /**
   * Next quilt in list
   */
  public Quilt next;

  /**
   * Makes new quilt with mapdesc
   * @param mapdesc map description
   */
  public Quilt(final Mapdesc mapdesc) {
    // DONE
    this.mapdesc = mapdesc;
    final Quiltspec[] tmpquilts = new Quiltspec[MAXDIM];
    for (int i = 0; i < tmpquilts.length; i++)
      tmpquilts[i] = new Quiltspec();
    this.qspec = new CArrayOfQuiltspecs(tmpquilts);

  }

  /**
   * Converts NURBS surface to bezier patches
   * @param sknotvector knots in u direction
   * @param tknotvector knots in v direction
   * @param ctrlarr control points
   * @param coords control points coords
   */
  public void toBezier(final Knotvector sknotvector, final Knotvector tknotvector,
                       final CArrayOfFloats ctrlarr, final int coords) {
    final Splinespec spline = new Splinespec(2);
    spline.kspecinit(sknotvector, tknotvector);
    spline.select();
    spline.layout(coords);
    spline.setupquilt(this);
    spline.copy(ctrlarr);
    spline.transform();
  }

  /**
   * Converts NURBS curve to list of bezier curves
   * @param knots knot vector
   * @param ctlarray control points
   * @param ncoords number of coordinates
   */
  public void toBezier(final Knotvector knots, final CArrayOfFloats ctlarray, final int ncoords) {
    // DONE
    final Splinespec spline = new Splinespec(1);
    spline.kspecinit(knots);
    spline.select();
    spline.layout(ncoords);
    spline.setupquilt(this);
    spline.copy(ctlarray);
    spline.transform();
  }

  /**
   * Walks thru all arcs/patches
   * @param pta low border
   * @param ptb high border
   * @param backend Backend
   */
  public void downloadAll(final float[] pta, final float[] ptb, final Backend backend) {
    // DONE
    for (Quilt m = this; m != null; m = m.next) {
      m.select(pta, ptb);
      m.download(backend);
    }

  }

  /**
   * Renders arcs/patches
   * @param backend Backend for rendering
   */
  private void download(final Backend backend) {
    // DONE
    if (getDimension() == 2) {

      final CArrayOfFloats ps = new CArrayOfFloats(cpts);
      ps.raisePointerBy(qspec.get(0).offset);
      ps.raisePointerBy(qspec.get(1).offset);
      ps.raisePointerBy(qspec.get(0).index * qspec.get(0).order
                        * qspec.get(0).stride);
      ps.raisePointerBy(qspec.get(1).index * qspec.get(1).order
                        * qspec.get(1).stride);

      backend.surfpts(mapdesc.getType(), ps, qspec.get(0).stride, qspec
                      .get(1).stride, qspec.get(0).order, qspec.get(1).order,
                      qspec.get(0).breakpoints[qspec.get(0).index],
                      qspec.get(0).breakpoints[qspec.get(0).index + 1], qspec
                      .get(1).breakpoints[qspec.get(1).index], qspec
                      .get(1).breakpoints[qspec.get(1).index + 1]);

    } else {// code for curves
      // CArrayOfFloats ps=new CArrayOfFloats(cpts);
      final CArrayOfFloats ps = new CArrayOfFloats(cpts.getArray(), 0);
      ps.raisePointerBy(qspec.get(0).offset);
      ps.raisePointerBy(qspec.get(0).index * qspec.get(0).order
                        * qspec.get(0).stride);
      backend.curvpts(mapdesc.getType(), ps, qspec.get(0).stride, qspec
                      .get(0).order,
                      qspec.get(0).breakpoints[qspec.get(0).index],
                      qspec.get(0).breakpoints[qspec.get(0).index + 1]);
    }

  }

  /**
   * Returns quilt dimension
   * @return quilt dimesion
   */
  private int getDimension() {
    // DONE
    return eqspec.getPointer() - qspec.getPointer();
  }

  /**
   * Finds Quiltspec.index
   * @param pta range
   * @param ptb range
   */
  private void select(final float[] pta, final float[] ptb) {
    // DONE
    final int dim = eqspec.getPointer() - qspec.getPointer();
    int i, j;
    for (i = 0; i < dim; i++) {
      for (j = qspec.get(i).width - 1; j >= 0; j--)
        if (qspec.get(i).breakpoints[j] <= pta[i]
            && ptb[i] <= qspec.get(i).breakpoints[j + 1])
          break;
      assert (j != -1);
      qspec.get(i).index = j;
    }
  }

  /**
   * Find range according to breakpoints
   * @param from low param
   * @param to high param
   * @param bpts breakpoints
   */
  public void getRange(final float[] from, final float[] to, final Flist bpts) {
    // DONE
    getRange(from, to, 0, bpts);

  }

  /**
   * Find range according to breakpoints
   * @param from low param
   * @param to high param
   * @param i from/to array index
   * @param list breakpoints
   */
  private void getRange(final float[] from, final float[] to, final int i, final Flist list) {
    // DONE
    final Quilt maps = this;
    from[i] = maps.qspec.get(i).breakpoints[0];
    to[i] = maps.qspec.get(i).breakpoints[maps.qspec.get(i).width];
    int maxpts = 0;
    Quilt m;
    for (m = maps; m != null; m = m.next) {
      if (m.qspec.get(i).breakpoints[0] > from[i])
        from[i] = m.qspec.get(i).breakpoints[0];
      if (m.qspec.get(i).breakpoints[m.qspec.get(i).width] < to[i])
        to[i] = m.qspec.get(i).breakpoints[m.qspec.get(i).width];
      maxpts += m.qspec.get(i).width + 1;
    }
    list.grow(maxpts);
    for (m = maps; m != null; m = m.next) {
      for (int j = 0; j <= m.qspec.get(i).width; j++) {
        list.add(m.qspec.get(i).breakpoints[j]);
      }
    }
    list.filter();
    list.taper(from[i], to[i]);
  }

  /**
   * Is this quilt culled
   * @return 0 or Subdivider.CULL_ACCEPT
   */
  public int isCulled() {
    if (mapdesc.isCulling()) {
      //                System.out.println("TODO quilt.isculled mapdesc.isculling");
      return 0;
    } else {
      return Subdivider.CULL_ACCEPT;
    }
  }

  /**
   * Finds range for surface
   * @param from low param
   * @param to high param
   * @param slist u direction breakpoints
   * @param tlist v direction breakpoints
   */
  public void getRange(final float[] from, final float[] to, final Flist slist, final Flist tlist) {
    // DONE
    getRange(from, to, 0, slist);
    getRange(from, to, 1, tlist);

  }

  /**
   * Empty method
   * @param sbrkpts
   * @param tbrkpts
   * @param rate
   */
  public void findRates(final Flist sbrkpts, final Flist tbrkpts, final float[] rate) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO quilt.findrates");
  }
}
