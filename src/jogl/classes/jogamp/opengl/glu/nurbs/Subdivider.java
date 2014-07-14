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
 * Class working with curves and surfaces
 * @author Tomas Hrasky
 *
 */
public class Subdivider {
  /**
   * Cull type
   */
  public static final int CULL_TRIVIAL_REJECT = 0;

  /**
   * Cull type
   */
  public static final int CULL_ACCEPT = 1;

  /**
   * Maximum trimming arcs
   */
  private static final int MAXARCS = 10;

  /**
   * Linked list of Quilts
   */
  Quilt qlist;

  /**
   * Object holding rendering honts information
   */
  private Renderhints renderhints;

  /**
   * Backend object
   */
  private Backend backend;

  /**
   * Number of subdivisions
   */
  private int subdivisions;

  /**
   * U step when using domain distance sampling
   */
  private float domain_distance_u_rate;

  /**
   * Use domain distance sampling
   */
  private int is_domain_distance_sampling;

  /**
   * Initial class holding trimming arcs
   */
  private Bin initialbin;

  /**
   * Not used
   */
  private boolean showDegenerate;

  /**
   * Is triming arc type bezier arc
   */
  private boolean isArcTypeBezier;

  /**
   * Breakpoints in v direction
   */
  private Flist tpbrkpts;

  /**
   * Breakpoints in u direction
   */
  private Flist spbrkpts;

  /**
   * Unused
   */
  private int s_index;

  /**
   * Head of linked list of trimming arcs
   */
  private Arc pjarc;

  /**
   * Class tesselating trimming arcs
   */
  private ArcTesselator arctesselator;

  /**
   * Unused
   */
  private int t_index;

  /**
   * Breakpoints
   */
  private final Flist smbrkpts = new Flist();

  /**
   * Not used
   * private float[] stepsizes;
   */

  /**
   * Domain distance in V direction
   */
  private float domain_distance_v_rate;

  /**
   * Initializes quilt list
   */
  public void beginQuilts(final Backend backend) {
    // DONE
    qlist = null;
    renderhints = new Renderhints();
    this.backend = backend;

    initialbin = new Bin();
    arctesselator = new ArcTesselator();
  }

  /**
   * Adds quilt to linked list
   * @param quilt added quilt
   */
  public void addQuilt(final Quilt quilt) {
    // DONE
    if (qlist == null)
      qlist = quilt;
    else {
      quilt.next = qlist;
      qlist = quilt;
    }

  }

  /**
   * Empty method
   */
  public void endQuilts() {
    // DONE
  }

  /**
   * Draws a surface
   */
  public void drawSurfaces() {
    renderhints.init();

    if (qlist == null) {
      //                System.out.println("qlist is null");
      return;
    }

    for (Quilt q = qlist; q != null; q = q.next) {
      if (q.isCulled() == CULL_TRIVIAL_REJECT) {
        freejarcs(initialbin);
        return;
      }
    }

    final float[] from = new float[2];
    final float[] to = new float[2];

    spbrkpts = new Flist();
    tpbrkpts = new Flist();
    qlist.getRange(from, to, spbrkpts, tpbrkpts);

    boolean optimize = (is_domain_distance_sampling > 0 && (renderhints.display_method != NurbsConsts.N_OUTLINE_PATCH));

    // TODO decide whether to optimize (when there is gluNurbsProperty implemented)
    optimize = true;

    if (!initialbin.isnonempty()) {
      if (!optimize) {
        makeBorderTrim(from, to);
      }
    } else {
      final float[] rate = new float[2];
      qlist.findRates(spbrkpts, tpbrkpts, rate);
      //                System.out.println("subdivider.drawsurfaces decompose");
    }

    backend.bgnsurf(renderhints.wiretris, renderhints.wirequads);

    // TODO partition test

    if (!initialbin.isnonempty() && optimize) {

      int i, j;
      int num_u_steps;
      int num_v_steps;
      for (i = spbrkpts.start; i < spbrkpts.end - 1; i++) {
        for (j = tpbrkpts.start; j < tpbrkpts.end - 1; j++) {
          final float[] pta = new float[2];
          final float[] ptb = new float[2];

          pta[0] = spbrkpts.pts[i];
          ptb[0] = spbrkpts.pts[i + 1];
          pta[1] = tpbrkpts.pts[j];
          ptb[1] = tpbrkpts.pts[j + 1];
          qlist.downloadAll(pta, ptb, backend);

          num_u_steps = (int) (domain_distance_u_rate * (ptb[0] - pta[0]));
          num_v_steps = (int) (domain_distance_v_rate * (ptb[1] - pta[1]));

          if (num_u_steps <= 0)
            num_u_steps = 1;
          if (num_v_steps <= 0)
            num_v_steps = 1;

          backend.surfgrid(pta[0], ptb[0], num_u_steps, ptb[1],
                           pta[1], num_v_steps);
          backend.surfmesh(0, 0, num_u_steps, num_v_steps);

        }
      }

    } else

      subdivideInS(initialbin);

    backend.endsurf();
  }

  /**
   * Empty method
   * @param initialbin2
   */
  private void freejarcs(final Bin initialbin2) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.freejarcs");
  }

  /**
   * Subdivide in U direction
   * @param source Trimming arcs source
   */
  private void subdivideInS(final Bin source) {
    // DONE
    if (renderhints.display_method == NurbsConsts.N_OUTLINE_PARAM) {
      outline(source);
      freejarcs(source);
    } else {
      setArcTypeBezier();
      setNonDegenerate();
      splitInS(source, spbrkpts.start, spbrkpts.end);
    }

  }

  /**
   * Split in U direction
   * @param source Trimming arcs source
   * @param start breakpoints start
   * @param end breakpoints end
   */
  private void splitInS(final Bin source, final int start, final int end) {
    // DONE
    if (source.isnonempty()) {
      if (start != end) {
        final int i = start + (end - start) / 2;
        final Bin left = new Bin();
        final Bin right = new Bin();

        split(source, left, right, 0, spbrkpts.pts[i]);
        splitInS(left, start, i);
        splitInS(right, i + 1, end);
      } else {
        if (start == spbrkpts.start || start == spbrkpts.end) {
          freejarcs(source);
        } else if (renderhints.display_method == NurbsConsts.N_OUTLINE_PARAM_S) {
          outline(source);
          freejarcs(source);
        } else {
          setArcTypeBezier();
          setNonDegenerate();
          s_index = start;
          splitInT(source, tpbrkpts.start, tpbrkpts.end);
        }
      }
    } else{
      //                System.out.println("Source is empty - subdivider.splitins");
    }
  }

  /**
   * Split in V direction
   * @param source
   * @param start
   * @param end
   */
  private void splitInT(final Bin source, final int start, final int end) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.splitint");

    if (source.isnonempty()) {
      if (start != end) {
        final int i = start + (end - start) / 2;
        final Bin left = new Bin();
        final Bin right = new Bin();
        split(source, left, right, 1, tpbrkpts.pts[i + 1]);
        splitInT(left, start, i);
        splitInT(right, i + 1, end);
      } else {
        if (start == tpbrkpts.start || start == tpbrkpts.end) {
          freejarcs(source);
        } else if (renderhints.display_method == NurbsConsts.N_OUTLINE_PARAM_ST) {
          outline(source);
          freejarcs(source);
        } else {
          t_index = start;
          setArcTypeBezier();
          setDegenerate();

          final float[] pta = new float[2];
          final float[] ptb = new float[2];

          pta[0] = spbrkpts.pts[s_index - 1];
          pta[1] = tpbrkpts.pts[t_index - 1];

          ptb[0] = spbrkpts.pts[s_index];
          ptb[1] = tpbrkpts.pts[t_index];
          qlist.downloadAll(pta, ptb, backend);

          final Patchlist patchlist = new Patchlist(qlist, pta, ptb);

          samplingSplit(source, patchlist,
                        renderhints.maxsubdivisions, 0);
          setNonDegenerate();
          setArcTypeBezier();
        }
      }
    }

  }

  /**
   * Sample
   * @param source
   * @param patchlist
   * @param subdivisions
   * @param param
   */
  private void samplingSplit(final Bin source, final Patchlist patchlist,
                             final int subdivisions, int param) {
    // DONE
    if (!source.isnonempty())
      return;
    if (patchlist.cullCheck() == CULL_TRIVIAL_REJECT) {
      freejarcs(source);
      return;
    }

    patchlist.getstepsize();
    if (renderhints.display_method == NurbsConsts.N_OUTLINE_PATCH) {
      tesselation(source, patchlist);
      outline(source);
      freejarcs(source);
      return;
    }

    tesselation(source, patchlist);
    if (patchlist.needsSamplingSubdivision() && subdivisions > 0) {
      if (!patchlist.needsSubdivision(0)) {
        param = 1;
      } else if (patchlist.needsSubdivision(1))
        param = 0;
      else
        param = 1 - param;

      final Bin left = new Bin();
      final Bin right = new Bin();

      final float mid = (float) ((patchlist.pspec[param].range[0] + patchlist.pspec[param].range[1]) * .5);

      split(source, left, right, param, mid);
      final Patchlist subpatchlist = new Patchlist(patchlist, param, mid);
      samplingSplit(left, subpatchlist, subdivisions - 1, param);
      samplingSplit(right, subpatchlist, subdivisions - 1, param);
    } else {
      setArcTypePwl();
      setDegenerate();
      nonSamplingSplit(source, patchlist, subdivisions, param);
      setDegenerate();
      setArcTypeBezier();
    }
  }

  /**
   * Not used
   * @param source
   * @param patchlist
   * @param subdivisions
   * @param param
   */
  private void nonSamplingSplit(final Bin source, final Patchlist patchlist,
                                final int subdivisions, int param) {
    // DONE
    if (patchlist.needsNonSamplingSubdivision() && subdivisions > 0) {
      param = 1 - param;

      final Bin left = new Bin();
      final Bin right = new Bin();

      final float mid = (float) ((patchlist.pspec[param].range[0] + patchlist.pspec[param].range[1]) * .5);
      split(source, left, right, param, mid);
      final Patchlist subpatchlist = new Patchlist(patchlist, param, mid);
      if (left.isnonempty()) {
        if (subpatchlist.cullCheck() == CULL_TRIVIAL_REJECT)
          freejarcs(left);
        else
          nonSamplingSplit(left, subpatchlist, subdivisions - 1,
                           param);
      }
      if (right.isnonempty()) {
        if (patchlist.cullCheck() == CULL_TRIVIAL_REJECT)
          freejarcs(right);
        else
          nonSamplingSplit(right, subpatchlist, subdivisions - 1,
                           param);
      }
    } else {
      patchlist.bbox();
      backend.patch(patchlist.pspec[0].range[0],
                    patchlist.pspec[0].range[1], patchlist.pspec[1].range[0],
                    patchlist.pspec[1].range[1]);
      if (renderhints.display_method == NurbsConsts.N_OUTLINE_SUBDIV) {
        outline(source);
        freejarcs(source);
      } else {
        setArcTypePwl();
        setDegenerate();
        findIrregularS(source);
        monosplitInS(source, smbrkpts.start, smbrkpts.end);
      }
    }

  }

  /**
   * Not used
   * @param source
   * @param start
   * @param end
   */
  private void monosplitInS(final Bin source, final int start, final int end) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.monosplitins");
  }

  /**
   * Not used
   * @param source
   */
  private void findIrregularS(final Bin source) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.findIrregularS");
  }

  /**
   * Not used
   */
  private void setArcTypePwl() {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.setarctypepwl");
  }

  /**
   * Not used
   * @param source
   * @param patchlist
   */
  private void tesselation(final Bin source, final Patchlist patchlist) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.tesselation");
  }

  /**
   * Not used
   */
  private void setDegenerate() {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.setdegenerate");
  }

  /**
   * Not used
   * @param bin
   * @param left
   * @param right
   * @param param
   * @param value
   */
  private void split(final Bin bin, final Bin left, final Bin right, final int param, final float value) {
    // DONE
    final Bin intersections = new Bin();
    final Bin unknown = new Bin();

    partition(bin, left, intersections, right, unknown, param, value);

    final int count = intersections.numarcs();
    // TODO jumpbuffer ??

    if (count % 2 == 0) {

      final Arc[] arclist = new Arc[MAXARCS];
      CArrayOfArcs list;
      if (count >= MAXARCS) {
        list = new CArrayOfArcs(new Arc[count]);
      } else {
        list = new CArrayOfArcs(arclist);
      }

      CArrayOfArcs last, lptr;
      Arc jarc;

      for (last = new CArrayOfArcs(list); (jarc = intersections
                                           .removearc()) != null; last.pp())
        last.set(jarc);

      if (param == 0) {// sort into incrasing t order
        final ArcSdirSorter sorter = new ArcSdirSorter(this);
        sorter.qsort(list, count);

        for (lptr = new CArrayOfArcs(list); lptr.getPointer() < last
               .getPointer(); lptr.raisePointerBy(2))
          check_s(lptr.get(), lptr.getRelative(1));
        for (lptr = new CArrayOfArcs(list); lptr.getPointer() < last
               .getPointer(); lptr.raisePointerBy(2))
          join_s(left, right, lptr.get(), lptr.getRelative(1));
        for (lptr = new CArrayOfArcs(list); lptr.getPointer() != last
               .getPointer(); lptr.pp()) {
          if (lptr.get().head()[0] <= value
              && lptr.get().tail()[0] <= value)
            left.addarc(lptr.get());
          else
            right.addarc(lptr.get());
        }

      } else {// sort into decreasing s order
        final ArcTdirSorter sorter = new ArcTdirSorter(this);
        sorter.qsort(list, count);

        for (lptr = new CArrayOfArcs(list); lptr.getPointer() < last
               .getPointer(); lptr.raisePointerBy(2))
          check_t(lptr.get(), lptr.getRelative(1));
        for (lptr = new CArrayOfArcs(list); lptr.getPointer() < last
               .getPointer(); lptr.raisePointerBy(2))
          join_t(left, right, lptr.get(), lptr.getRelative(1));
        for (lptr = new CArrayOfArcs(list); lptr.getPointer() != last
               .getPointer(); lptr.raisePointerBy(2)) {
          if (lptr.get().head()[0] <= value
              && lptr.get().tail()[0] <= value)
            left.addarc(lptr.get());
          else
            right.addarc(lptr.get());
        }

      }

      unknown.adopt();
    }
  }

  /**
   * Not used
   * @param left
   * @param right
   * @param arc
   * @param relative
   */
  private void join_t(final Bin left, final Bin right, final Arc arc, final Arc relative) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.join_t");
  }

  /**
   * Not used
   * @param arc
   * @param relative
   */
  private void check_t(final Arc arc, final Arc relative) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.check_t");
  }

  /**
   * Not used
   * @param left
   * @param right
   * @param jarc1
   * @param jarc2
   */
  private void join_s(final Bin left, final Bin right, Arc jarc1, Arc jarc2) {
    // DONE
    if (!jarc1.getitail())
      jarc1 = jarc1.next;
    if (!jarc2.getitail())
      jarc2 = jarc2.next;

    final float s = jarc1.tail()[0];
    final float t1 = jarc1.tail()[1];
    final float t2 = jarc2.tail()[1];

    if (t1 == t2) {
      simplelink(jarc1, jarc2);
    } else {
      final Arc newright = new Arc(Arc.ARC_RIGHT);
      final Arc newleft = new Arc(Arc.ARC_LEFT);
      if (isBezierArcType()) {
        arctesselator.bezier(newright, s, s, t1, t2);
        arctesselator.bezier(newleft, s, s, t2, t1);
      } else {
        arctesselator.pwl_right(newright, s, t1, t2, 0 /* stepsizes[0] */);
        arctesselator.pwl_left(newright, s, t2, t1, 0 /* stepsizes[2] */);
      }
      link(jarc1, jarc2, newright, newleft);
      left.addarc(newright);
      right.addarc(newleft);
    }

  }

  /**
   * Not used
   * @param jarc1
   * @param jarc2
   * @param newright
   * @param newleft
   */
  private void link(final Arc jarc1, final Arc jarc2, final Arc newright, final Arc newleft) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.link");
  }

  /**
   * Not used
   * @return true
   */
  private boolean isBezierArcType() {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.isbezierarc");
    return true;
  }

  /**
   * Not used
   * @param jarc1
   * @param jarc2
   */
  private void simplelink(final Arc jarc1, final Arc jarc2) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.simplelink");
  }

  /**
   * Not used
   * @param arc
   * @param relative
   */
  private void check_s(final Arc arc, final Arc relative) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.check_s");

  }

  /**
   * Not used
   * @param bin
   * @param left
   * @param intersections
   * @param right
   * @param unknown
   * @param param
   * @param value
   */
  private void partition(final Bin bin, final Bin left, final Bin intersections, final Bin right,
                         final Bin unknown, final int param, final float value) {

    final Bin headonleft = new Bin();
    final Bin headonright = new Bin();
    final Bin tailonleft = new Bin();
    final Bin tailonright = new Bin();

    for (Arc jarc = bin.removearc(); jarc != null; jarc = bin.removearc()) {
      final float tdiff = jarc.tail()[param] - value;
      final float hdiff = jarc.head()[param] - value;

      if (tdiff > 0) {
        if (hdiff > 0) {
          right.addarc(jarc);
        } else if (hdiff == 0) {
          tailonright.addarc(jarc);
        } else {
          switch (arc_split(jarc, param, value, 0)) {
          case 2:
            tailonright.addarc(jarc);
            headonleft.addarc(jarc.next);
            break;
            // TODO rest cases
          default:
            System.out
              .println("TODO subdivider.partition rest cases");
            break;
          }
        }
      } else if (tdiff == 0) {
        if (hdiff > 0) {
          headonright.addarc(jarc);
        } else if (hdiff == 0) {
          unknown.addarc(jarc);
        } else {
          headonright.addarc(jarc);
        }
      } else {
        if (hdiff > 0) {
          // TODO rest
          //                        System.out.println("TODO subdivider.partition rest of else");
        } else if (hdiff == 0) {
          tailonleft.addarc(jarc);
        } else {
          left.addarc(jarc);
        }
      }

    }
    if (param == 0) {
      classify_headonleft_s(headonleft, intersections, left, value);
      classify_tailonleft_s(tailonleft, intersections, left, value);
      classify_headonright_s(headonright, intersections, right, value);
      classify_tailonright_s(tailonright, intersections, right, value);
    } else {
      classify_headonleft_t(headonleft, intersections, left, value);
      classify_tailonleft_t(tailonleft, intersections, left, value);
      classify_headonright_t(headonright, intersections, right, value);
      classify_tailonright_t(tailonright, intersections, right, value);
    }
  }

  /**
   * Not used
   * @param tailonright
   * @param intersections
   * @param right
   * @param value
   */
  private void classify_tailonright_t(final Bin tailonright, final Bin intersections,
                                      final Bin right, final float value) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.classify_tailonright_t");

  }

  /**
   * Not used
   * @param bin
   * @param in
   * @param out
   * @param val
   */
  private void classify_tailonleft_s(final Bin bin, final Bin in, final Bin out, final float val) {

    // DONE
    Arc j;
    while ((j = bin.removearc()) != null) {
      j.clearitail();

      final float diff = j.next.head()[0] - val;
      if (diff > 0) {
        in.addarc(j);
      } else if (diff < 0) {
        if (ccwTurn_sl(j, j.next))
          out.addarc(j);
        else
          in.addarc(j);
      } else {
        if (j.next.tail()[1] > j.next.head()[1])
          in.addarc(j);
        else
          out.addarc(j);
      }
    }

  }

  /**
   * Not used
   * @param bin
   * @param in
   * @param out
   * @param val
   */
  private void classify_headonright_s(final Bin bin, final Bin in, final Bin out, final float val) {
    // DONE
    Arc j;
    while ((j = bin.removearc()) != null) {
      j.setitail();

      final float diff = j.prev.tail()[0] - val;
      if (diff > 0) {
        if (ccwTurn_sr(j.prev, j))
          out.addarc(j);
        else
          in.addarc(j);
      } else if (diff < 0) {
        out.addarc(j);
      } else {
        if (j.prev.tail()[1] > j.prev.head()[1])
          out.addarc(j);
        else
          in.addarc(j);
      }
    }
  }

  /**
   * Not used
   * @param prev
   * @param j
   * @return false
   */
  private boolean ccwTurn_sr(final Arc prev, final Arc j) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO ccwTurn_sr");
    return false;
  }

  /**
   * Not used
   * @param headonright
   * @param intersections
   * @param right
   * @param value
   */
  private void classify_headonright_t(final Bin headonright, final Bin intersections,
                                      final Bin right, final float value) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.classify_headonright_t");
  }

  /**
   * Not used
   * @param tailonleft
   * @param intersections
   * @param left
   * @param value
   */
  private void classify_tailonleft_t(final Bin tailonleft, final Bin intersections,
                                     final Bin left, final float value) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.classify_tailonleft_t");
  }

  /**
   * Not used
   * @param bin
   * @param in
   * @param out
   * @param val
   */
  private void classify_headonleft_t(final Bin bin, final Bin in, final Bin out, final float val) {
    // DONE
    Arc j;
    while ((j = bin.removearc()) != null) {
      j.setitail();

      final float diff = j.prev.tail()[1] - val;
      if (diff > 0) {
        out.addarc(j);
      } else if (diff < 0) {
        if (ccwTurn_tl(j.prev, j))
          out.addarc(j);
        else
          in.addarc(j);
      } else {
        if (j.prev.tail()[0] > j.prev.head()[0])
          out.addarc(j);
        else
          in.addarc(j);
      }
    }
  }

  /**
   * Not used
   * @param prev
   * @param j
   * @return false
   */
  private boolean ccwTurn_tl(final Arc prev, final Arc j) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.ccwTurn_tl");
    return false;
  }

  /**
   * Not used
   * @param bin
   * @param in
   * @param out
   * @param val
   */
  private void classify_tailonright_s(final Bin bin, final Bin in, final Bin out, final float val) {
    // DONE
    Arc j;
    while ((j = bin.removearc()) != null) {
      j.clearitail();

      final float diff = j.next.head()[0] - val;
      if (diff > 0) {
        if (ccwTurn_sr(j, j.next))
          out.addarc(j);
        else
          in.addarc(j);
      } else if (diff < 0) {
        in.addarc(j);
      } else {
        if (j.next.tail()[1] > j.next.head()[1])
          out.addarc(j);
        else
          in.addarc(j);
      }
    }

  }

  /**
   * Not used
   * @param bin
   * @param in
   * @param out
   * @param val
   */
  private void classify_headonleft_s(final Bin bin, final Bin in, final Bin out, final float val) {
    // DONE
    Arc j;
    while ((j = bin.removearc()) != null) {
      j.setitail();

      final float diff = j.prev.tail()[0] - val;
      if (diff > 0) {
        out.addarc(j);
      } else if (diff < 0) {
        if (ccwTurn_sl(j.prev, j))
          out.addarc(j);
        else
          in.addarc(j);
      } else {
        if (j.prev.tail()[1] > j.prev.head()[1])
          in.addarc(j);
        else
          out.addarc(j);
      }
    }

  }

  /**
   * Not used
   * @param prev
   * @param j
   * @return false
   */
  private boolean ccwTurn_sl(final Arc prev, final Arc j) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.ccwTurn_sl");
    return false;
  }

  /**
   * Not used
   * @param jarc
   * @param param
   * @param value
   * @param i
   * @return 0
   */
  private int arc_split(final Arc jarc, final int param, final float value, final int i) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.arc_split");
    return 0;
  }

  /**
   * Not used
   */
  private void setNonDegenerate() {
    // DONE
    this.showDegenerate = false;

  }

  /**
   * sets trimming arc default type to bezier
   */
  private void setArcTypeBezier() {
    // DONE
    isArcTypeBezier = true;
  }

  /**
   * Not used
   * @param source
   */
  private void outline(final Bin source) {
    // TODO Auto-generated method stub
    //            System.out.println("TODO subdivider.outline");
  }

  /**
   * Makes default trim along surface borders
   * @param from range beginnings
   * @param to range ends
   */
  private void makeBorderTrim(final float[] from, final float[] to) {
    // DONE
    final float smin = from[0];
    final float smax = to[0];

    final float tmin = from[1];
    final float tmax = to[1];

    pjarc = null;
    Arc jarc = null;

    jarc = new Arc(Arc.ARC_BOTTOM);
    arctesselator.bezier(jarc, smin, smax, tmin, tmin);
    initialbin.addarc(jarc);
    pjarc = jarc.append(pjarc);

    jarc = new Arc(Arc.ARC_RIGHT);
    arctesselator.bezier(jarc, smax, smax, tmin, tmax);
    initialbin.addarc(jarc);
    pjarc = jarc.append(pjarc);

    jarc = new Arc(Arc.ARC_TOP);
    arctesselator.bezier(jarc, smax, smin, tmax, tmax);
    initialbin.addarc(jarc);
    pjarc = jarc.append(pjarc);

    jarc = new Arc(Arc.ARC_LEFT);
    arctesselator.bezier(jarc, smin, smin, tmax, tmin);
    initialbin.addarc(jarc);
    jarc = jarc.append(pjarc);

    // assert (jarc.check() == true);
  }

  /**
   * Draws NURBS curve
   */
  public void drawCurves() {
    // DONE
    final float[] from = new float[1];
    final float[] to = new float[1];

    final Flist bpts = new Flist();
    qlist.getRange(from, to, bpts);

    renderhints.init();

    backend.bgncurv();

    for (int i = bpts.start; i < bpts.end - 1; i++) {
      final float[] pta = new float[1];
      final float[] ptb = new float[1];
      pta[0] = bpts.pts[i];
      ptb[0] = bpts.pts[i + 1];

      qlist.downloadAll(pta, ptb, backend);
      final Curvelist curvelist = new Curvelist(qlist, pta, ptb);
      samplingSplit(curvelist, renderhints.maxsubdivisions);
    }
    backend.endcurv();
  }

  /**
   * Samples a curve in case of need, or sends curve to backend
   * @param curvelist list of curves
   * @param maxsubdivisions maximum number of subdivisions
   */
  private void samplingSplit(final Curvelist curvelist, final int maxsubdivisions) {
    if (curvelist.cullCheck() == CULL_TRIVIAL_REJECT)
      return;

    curvelist.getstepsize();

    if (curvelist.needsSamplingSubdivision() && (subdivisions > 0)) {
      // TODO kód
      //                System.out.println("TODO subdivider-needsSamplingSubdivision");
    } else {
      final int nu = (int) (1 + curvelist.range[2] / curvelist.stepsize);
      backend.curvgrid(curvelist.range[0], curvelist.range[1], nu);
      backend.curvmesh(0, nu);
    }

  }

  /**
   * Sets new domain_distance_u_rate value
   * @param d new domain_distance_u_rate value

  */
  public void set_domain_distance_u_rate(final double d) {
    // DONE
    domain_distance_u_rate = (float) d;
  }

  /**
   * Sets new domain_distance_v_rate value
   * @param d new domain_distance_v_rate value
   */
  public void set_domain_distance_v_rate(final double d) {
    // DONE
    domain_distance_v_rate = (float) d;
  }

  /**
   * Sets new is_domain_distance_sampling value
   * @param i new is_domain_distance_sampling value
   */
  public void set_is_domain_distance_sampling(final int i) {
    // DONE
    this.is_domain_distance_sampling = i;
  }
}
