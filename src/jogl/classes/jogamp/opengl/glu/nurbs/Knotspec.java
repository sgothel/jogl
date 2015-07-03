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
 * Knot vector specification
 *
 * @author Tomas Hrasky
 *
 */
public class Knotspec {

  /**
   * Begin of input knots
   */
  public CArrayOfFloats inkbegin;

  /**
   * End of input knots
   */
  public CArrayOfFloats inkend;

  /**
   * Stride before knot operations
   */
  public int prestride;

  /**
   * Curve order
   */
  public int order;

  /**
   * Next knot specification in linked list (used in surfaces)
   */
  public Knotspec next;

  /**
   * Last knot
   */
  public CArrayOfFloats klast;

  /**
   * First knot
   */
  CArrayOfFloats kfirst;

  /**
   * Beginning of breakpoints
   */
  CArrayOfBreakpts bbegin;

  /**
   * End of breakpoints
   */
  CArrayOfBreakpts bend;

  /**
   * Considered left end knot
   */
  CArrayOfFloats kleft;

  /**
   * Considered right end knot
   */
  CArrayOfFloats kright;

  /**
   * Offset before knot operations
   */
  int preoffset;

  /**
   * Control points array Length after knot operations
   */
  int postwidth;

  /**
   * Beginning of coeficients array
   */
  private CArrayOfFloats sbegin;

  /**
   * Beginning of output knots
   */
  private CArrayOfFloats outkbegin;

  /**
   * End of output knots
   */
  private CArrayOfFloats outkend;

  /**
   * Control points aray length before knot operations
   */
  int prewidth;

  /**
   * Offset after knot operations
   */
  int postoffset;

  /**
   * Number of control points' coordinates after knot operations
   */
  public int poststride;

  /**
   * Number of control points' coordinates
   */
  public int ncoords;

  /**
   * Tell whether knotspec has already benn transformed
   */
  public boolean istransformed;

  /**
   * Knotspec to be transformed
   */
  public Knotspec kspectotrans;

  /**
   * Finds knot border of knot insertion and required multiplicities
   */
  public void preselect() {
    // DONE
    float kval;

    klast = new CArrayOfFloats(inkend);
    klast.lessenPointerBy(order);
    for (kval = klast.get(); klast.getPointer() != inkend.getPointer(); klast
           .pp()) {
      if (!Knotvector.identical(klast.get(), kval))
        break;
    }

    kfirst = new CArrayOfFloats(inkbegin);
    kfirst.raisePointerBy(order - 1);
    for (kval = kfirst.get(); kfirst.getPointer() != inkend.getPointer(); kfirst
           .pp()) {
      if (!Knotvector.identical(kfirst.get(), kval))
        break;
    }

    final CArrayOfFloats k = new CArrayOfFloats(kfirst);
    k.mm();

    for (; k.getPointer() >= inkbegin.getPointer(); k.mm())
      if (!Knotvector.identical(kval, k.get()))
        break;
    k.pp();

    final Breakpt[] bbeginArray = new Breakpt[(klast.getPointer() - kfirst
                                         .getPointer()) + 1];
    for (int i = 0; i < bbeginArray.length; i++)
      bbeginArray[i] = new Breakpt();
    bbegin = new CArrayOfBreakpts(bbeginArray, 0);
    bbegin.get().multi = kfirst.getPointer() - k.getPointer();
    bbegin.get().value = kval;

    bend = new CArrayOfBreakpts(bbegin);
    kleft = new CArrayOfFloats(kfirst);
    kright = new CArrayOfFloats(kfirst);

  }

  /**
   * Perpares knotspec for transformation
   */
  public void select() {
    // DONE
    breakpoints();
    knots();
    factors();

    preoffset = kleft.getPointer() - (inkbegin.getPointer() + order);
    postwidth = ((bend.getPointer() - bbegin.getPointer()) * order);
    prewidth = (outkend.getPointer() - outkbegin.getPointer()) - order;
    postoffset = (bbegin.get().def > 1) ? (bbegin.get().def - 1) : 0;

  }

  /**
   * Computes alpha factors for computing new control points
   */
  private void factors() {
    // DONE
    final CArrayOfFloats mid = new CArrayOfFloats(outkend.getArray(), (outkend
                                                                 .getPointer() - 1)
                                            - order + bend.get().multi);

    CArrayOfFloats fptr = null;
    if (sbegin != null)
      fptr = new CArrayOfFloats(sbegin);

    for (final CArrayOfBreakpts bpt = new CArrayOfBreakpts(bend); bpt
           .getPointer() >= bbegin.getPointer(); bpt.mm()) {
      mid.lessenPointerBy(bpt.get().multi);
      final int def = bpt.get().def - 1;
      if (def < 0)
        continue;
      final float kv = bpt.get().value;

      final CArrayOfFloats kf = new CArrayOfFloats(mid.getArray(), (mid
                                                              .getPointer() - def)
                                             + (order - 1));
      for (final CArrayOfFloats kl = new CArrayOfFloats(kf.getArray(), kf
                                                  .getPointer()
                                                  + def); kl.getPointer() != kf.getPointer(); kl.mm()) {
        CArrayOfFloats kh, kt;
        for (kt = new CArrayOfFloats(kl), kh = new CArrayOfFloats(mid); kt
               .getPointer() != kf.getPointer(); kh.mm(), kt.mm()) {
          fptr.set((kv - kh.get()) / (kt.get() - kh.get()));
          fptr.pp();
        }
        kl.set(kv);
      }
    }

  }

  /**
   * Makes new knot vector
   */
  private void knots() {
    // DONE
    final CArrayOfFloats inkpt = new CArrayOfFloats(kleft.getArray(), kleft
                                              .getPointer()
                                              - order);
    final CArrayOfFloats inkend = new CArrayOfFloats(kright.getArray(), kright
                                               .getPointer()
                                               + bend.get().def);

    outkbegin = new CArrayOfFloats(new float[inkend.getPointer()
                                             - inkpt.getPointer()], 0);
    CArrayOfFloats outkpt;
    for (outkpt = new CArrayOfFloats(outkbegin); inkpt.getPointer() != inkend
           .getPointer(); inkpt.pp(), outkpt.pp()) {
      outkpt.set(inkpt.get());
    }
    outkend = new CArrayOfFloats(outkpt);
  }

  /**
   * Analyzes breakpoints
   */
  private void breakpoints() {
    // DONE
    final CArrayOfBreakpts ubpt = new CArrayOfBreakpts(bbegin);
    final CArrayOfBreakpts ubend = new CArrayOfBreakpts(bend);
    int nfactors = 0;

    ubpt.get().value = ubend.get().value;
    ubpt.get().multi = ubend.get().multi;

    kleft = new CArrayOfFloats(kright);

    for (; kright.getPointer() != klast.getPointer(); kright.pp()) {
      if (Knotvector.identical(kright.get(), ubpt.get().value)) {
        ubpt.get().multi++;
      } else {
        ubpt.get().def = order - ubpt.get().multi;
        nfactors += (ubpt.get().def * (ubpt.get().def - 1)) / 2;
        ubpt.pp();
        ubpt.get().value = kright.get();
        ubpt.get().multi = 1;
      }
    }
    ubpt.get().def = order - ubpt.get().multi;
    nfactors += (ubpt.get().def * (ubpt.get().def - 1)) / 2;

    bend = new CArrayOfBreakpts(ubpt);

    if (nfactors > 0) {
      sbegin = new CArrayOfFloats(new float[nfactors], 0);
    } else {
      sbegin = null;
    }

  }

  /**
   * Copies control points
   *
   * @param _inpt
   *            input control points
   * @param _outpt
   *            output control points
   */
  public void copy(final CArrayOfFloats _inpt, final CArrayOfFloats _outpt) {
    final CArrayOfFloats inpt = new CArrayOfFloats(_inpt);
    final CArrayOfFloats outpt = new CArrayOfFloats(_outpt);

    inpt.raisePointerBy(preoffset);
    if (next != null) {
      for (final CArrayOfFloats lpt = new CArrayOfFloats(outpt.getArray(),
                                                   outpt.getPointer() + prewidth); outpt.getPointer() != lpt
             .getPointer(); outpt.raisePointerBy(poststride)) {
        next.copy(inpt, outpt);
        inpt.raisePointerBy(prestride);
      }

    } else {
      for (final CArrayOfFloats lpt = new CArrayOfFloats(outpt.getArray(),
                                                   outpt.getPointer() + prewidth); outpt.getPointer() != lpt
             .getPointer(); outpt.raisePointerBy(poststride)) {
        pt_io_copy(outpt, inpt);
        inpt.raisePointerBy(prestride);
      }
    }

  }

  /**
   * Copies one control point to other
   *
   * @param topt
   *            source control point
   * @param frompt
   *            destination control point
   */
  private void pt_io_copy(final CArrayOfFloats topt, final CArrayOfFloats frompt) {
    // DONE
    switch (ncoords) {
    case 4:
      topt.setRelative(3, frompt.getRelative(3));
    case 3:
      topt.setRelative(2, frompt.getRelative(2));
    case 2:
      topt.setRelative(1, frompt.getRelative(1));
    case 1:
      topt.set(frompt.get());
      break;
    default:
      // TODO break with copying in general case
      //                System.out.println("TODO knotspec.pt_io_copy");
      break;
    }

  }

  /**
   * Inserts a knot
   *
   * @param _p
   *            inserted knot
   */
  public void transform(final CArrayOfFloats _p) {
    final CArrayOfFloats p = new CArrayOfFloats(_p);
    // DONE
    if (next != null) {//surface code
      if (this.equals(kspectotrans)) {
        next.transform(p);
      } else {
        if (istransformed) {
          p.raisePointerBy(postoffset);
          for (final CArrayOfFloats pend = new CArrayOfFloats(p.getArray(),
                            p.getPointer() + postwidth); p.getPointer() != pend
                 .getPointer(); p.raisePointerBy(poststride))
            next.transform(p);

        } else {
          final CArrayOfFloats pend = new CArrayOfFloats(p.getArray(), p
                                                   .getPointer()
                                                   + prewidth);
          for (; p.getPointer() != pend.getPointer(); p
                 .raisePointerBy(poststride))
            next.transform(p);
        }
      }

    } else {//code for curve
      if (this.equals(kspectotrans)) {
        insert(p);
      } else {
        if (istransformed) {
          p.raisePointerBy(postoffset);
          for (final CArrayOfFloats pend = new CArrayOfFloats(p.getArray(),
                            p.getPointer() + postwidth); p.getPointer() != pend
                 .getPointer(); p.raisePointerBy(poststride)) {
            kspectotrans.insert(p);
          }
        } else {
          final CArrayOfFloats pend = new CArrayOfFloats(p.getArray(), p
                                                   .getPointer()
                                                   + prewidth);
          for (; p.getPointer() != pend.getPointer(); p
                 .raisePointerBy(poststride))
            kspectotrans.insert(p);
        }
      }
    }

  }

  /**
   * Inserts a knot and computes new control points
   *
   * @param p
   *            inserted knot
   */
  private void insert(final CArrayOfFloats p) {
    // DONE
    CArrayOfFloats fptr = null;
    if (sbegin != null)
      fptr = new CArrayOfFloats(sbegin);
    final CArrayOfFloats srcpt = new CArrayOfFloats(p.getArray(), p.getPointer()
                                              + prewidth - poststride);
    // CArrayOfFloats srcpt = new CArrayOfFloats(p.getArray(), prewidth -
    // poststride);
    final CArrayOfFloats dstpt = new CArrayOfFloats(p.getArray(), p.getPointer()
                                              + postwidth + postoffset - poststride);
    // CArrayOfFloats dstpt = new CArrayOfFloats(p.getArray(), postwidth +
    // postoffset - poststride);
    final CArrayOfBreakpts bpt = new CArrayOfBreakpts(bend);

    for (final CArrayOfFloats pend = new CArrayOfFloats(srcpt.getArray(), srcpt
                                                  .getPointer()
                                                  - poststride * bpt.get().def); srcpt.getPointer() != pend
           .getPointer(); pend.raisePointerBy(poststride)) {
      final CArrayOfFloats p1 = new CArrayOfFloats(srcpt);
      for (final CArrayOfFloats p2 = new CArrayOfFloats(srcpt.getArray(), srcpt
                                                  .getPointer()
                                                  - poststride); p2.getPointer() != pend.getPointer(); p1
             .setPointer(p2.getPointer()), p2
             .lessenPointerBy(poststride)) {
        pt_oo_sum(p1, p1, p2, fptr.get(), 1.0 - fptr.get());
        fptr.pp();
      }
    }
    bpt.mm();
    for (; bpt.getPointer() >= bbegin.getPointer(); bpt.mm()) {

      for (int multi = bpt.get().multi; multi > 0; multi--) {
        pt_oo_copy(dstpt, srcpt);
        dstpt.lessenPointerBy(poststride);
        srcpt.lessenPointerBy(poststride);
      }
      for (final CArrayOfFloats pend = new CArrayOfFloats(srcpt.getArray(),
                                                    srcpt.getPointer() - poststride * bpt.get().def); srcpt
             .getPointer() != pend.getPointer(); pend
             .raisePointerBy(poststride), dstpt
             .lessenPointerBy(poststride)) {
        pt_oo_copy(dstpt, srcpt);
        final CArrayOfFloats p1 = new CArrayOfFloats(srcpt);

        for (final CArrayOfFloats p2 = new CArrayOfFloats(srcpt.getArray(),
                                                    srcpt.getPointer() - poststride); p2.getPointer() != pend
               .getPointer(); p1.setPointer(p2.getPointer()), p2
               .lessenPointerBy(poststride)) {
          pt_oo_sum(p1, p1, p2, fptr.get(), 1.0 - fptr.get());
          fptr.pp();
        }
      }
    }
  }

  /**
   * Copies one control point to another
   *
   * @param topt
   *            source ctrl point
   * @param frompt
   *            distance ctrl point
   */
  private void pt_oo_copy(final CArrayOfFloats topt, final CArrayOfFloats frompt) {
    // DONE
    // this is a "trick" with case - "break" is omitted so it comes through all cases
    switch (ncoords) {
    case 4:
      topt.setRelative(3, frompt.getRelative(3));
    case 3:
      topt.setRelative(2, frompt.getRelative(2));
    case 2:
      topt.setRelative(1, frompt.getRelative(1));
    case 1:
      topt.setRelative(0, frompt.getRelative(0));
      break;
    default:
      // default uses memcpy but it is not needed (we probably won't have more than 4 coords)
      // TODO not sure about it
      break;
    }

  }

  /**
   * Computes new control point
   *
   * @param x
   *            first point
   * @param y
   *            second point
   * @param z
   *            third pont
   * @param a
   *            alpha
   * @param b
   *            1 - alpha
   */
  private void pt_oo_sum(final CArrayOfFloats x, final CArrayOfFloats y,
                         final CArrayOfFloats z, final float a, final double b) {
    // DONE
    switch (ncoords) {
    case 4:
      x.setRelative(3, (float) (a * y.getRelative(3) + b
                                * z.getRelative(3)));
    case 3:
      x.setRelative(2, (float) (a * y.getRelative(2) + b
                                * z.getRelative(2)));
    case 2:
      x.setRelative(1, (float) (a * y.getRelative(1) + b
                                * z.getRelative(1)));
    case 1:
      x.setRelative(0, (float) (a * y.getRelative(0) + b
                                * z.getRelative(0)));
      break;
    default:
      //no need of default - see previous method and its case statement
      //                System.out.println("TODO pt_oo_sum default");
      break;
    }
  }
}
