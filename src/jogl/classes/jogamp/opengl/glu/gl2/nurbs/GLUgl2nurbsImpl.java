package jogamp.opengl.glu.gl2.nurbs;
import jogamp.opengl.glu.nurbs.*;

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

import java.lang.reflect.Method;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLUnurbs;

/**
 * Base object for working with NURBS curves and surfaces
 * 
 * @author Tomas Hrasky
 * 
 */
public class GLUgl2nurbsImpl implements GLUnurbs {

  /**
   * Curve type - no type
   */
  public static final int CT_NONE = 0;

  /**
   * Curve type - NURBS curve
   */
  public static final int CT_NURBSCURVE = 1;

  /**
   * Curve type - picewise linear curve
   */
  public static final int CT_NPWLCURVE = 2;

  /**
   * Matrixes autoloading
   */
  private boolean autoloadmode;

  /**
   * Using callback
   */
  private int callBackFlag;

  /**
   * Object for error call backs
   */
  private Object errorCallback;

  /**
   * List of map definitions
   */
  Maplist maplist;

  /**
   * Indicates validity of data
   */
  private int isDataValid;

  /**
   * Are we in the middle of curve processing
   */
  private int inCurve;

  /**
   * Current curve
   */
  private O_curve currentCurve;

  /**
   * Are we in trim
   */
  private boolean inTrim;

  /**
   * Are we playbacking curve/surface rendering
   */
  private boolean playBack;

  /**
   * Next curve in linked list
   */
  private O_curve nextCurve;

  /**
   * Is curve modified
   */
  private int isCurveModified;

  /**
   * Object holding rendering settings
   */
  private Renderhints renderhints;

  /**
   * Display list
   */
  private DisplayList dl;

  /**
   * Object for subdividing curves and surfaces
   */
  private Subdivider subdivider;

  /**
   * Object responsible for rendering
   */
  private Backend backend;

  /**
   * Next picewise linear curve in linked list
   */
  private O_pwlcurve nextPwlcurve;

  /**
   * Next trimming NURBS curve in linked list
   */
  private O_nurbscurve nextNurbscurve;

  /**
   * Are we in the middle of surface processing
   */
  private int inSurface;

  /**
   * Are there any changes in trimming
   */
  private boolean isTrimModified;

  /**
   * Are there any changes in surface data
   */
  private boolean isDataSurfaceModified;

  /**
   * Nurber of trmims of processed surface
   */
  private int numTrims;

  /**
   * Current processed surface
   */
  private O_surface currentSurface;

  /**
   * Next trimming curve
   */
  private O_trim nextTrim;

  /**
   * Nextr surface in linked list
   */
  private O_nurbssurface nextNurbssurface;

  /**
   * Are there any changes in surface
   */
  private boolean isSurfaceModified;

  /**
   * Initializes default GLUgl2nurbs object
   */
  public GLUgl2nurbsImpl() {
    // DONE
    maplist = new Maplist(backend);
    renderhints = new Renderhints();
    subdivider = new Subdivider();
    // original code

    redefineMaps();

    defineMap(GL2.GL_MAP2_NORMAL, 0, 3);
    defineMap(GL2.GL_MAP1_NORMAL, 0, 3);
    defineMap(GL2.GL_MAP2_TEXTURE_COORD_1, 0, 1);
    defineMap(GL2.GL_MAP1_TEXTURE_COORD_1, 0, 1);
    defineMap(GL2.GL_MAP2_TEXTURE_COORD_2, 0, 2);
    defineMap(GL2.GL_MAP1_TEXTURE_COORD_2, 0, 2);
    defineMap(GL2.GL_MAP2_TEXTURE_COORD_3, 0, 3);
    defineMap(GL2.GL_MAP1_TEXTURE_COORD_3, 0, 3);
    defineMap(GL2.GL_MAP2_TEXTURE_COORD_4, 1, 4);
    defineMap(GL2.GL_MAP1_TEXTURE_COORD_4, 1, 4);
    defineMap(GL2.GL_MAP2_VERTEX_4, 1, 4);
    defineMap(GL2.GL_MAP1_VERTEX_4, 1, 4);
    defineMap(GL2.GL_MAP2_VERTEX_3, 0, 3);
    defineMap(GL2.GL_MAP1_VERTEX_3, 0, 3);
    defineMap(GL2.GL_MAP2_COLOR_4, 0, 4);
    defineMap(GL2.GL_MAP1_COLOR_4, 0, 4);
    defineMap(GL2.GL_MAP2_INDEX, 0, 1);
    defineMap(GL2.GL_MAP1_INDEX, 0, 1);

    setnurbsproperty(GL2.GL_MAP1_VERTEX_3, NurbsConsts.N_SAMPLINGMETHOD,
                     (float) NurbsConsts.N_PATHLENGTH);
    setnurbsproperty(GL2.GL_MAP1_VERTEX_4, NurbsConsts.N_SAMPLINGMETHOD,
                     (float) NurbsConsts.N_PATHLENGTH);
    setnurbsproperty(GL2.GL_MAP2_VERTEX_3, NurbsConsts.N_SAMPLINGMETHOD,
                     (float) NurbsConsts.N_PATHLENGTH);
    setnurbsproperty(GL2.GL_MAP2_VERTEX_4, NurbsConsts.N_SAMPLINGMETHOD,
                     (float) NurbsConsts.N_PATHLENGTH);

    setnurbsproperty(GL2.GL_MAP1_VERTEX_3, NurbsConsts.N_PIXEL_TOLERANCE,
                     (float) 50.0);
    setnurbsproperty(GL2.GL_MAP1_VERTEX_4, NurbsConsts.N_PIXEL_TOLERANCE,
                     (float) 50.0);
    setnurbsproperty(GL2.GL_MAP2_VERTEX_3, NurbsConsts.N_PIXEL_TOLERANCE,
                     (float) 50.0);
    setnurbsproperty(GL2.GL_MAP2_VERTEX_4, NurbsConsts.N_PIXEL_TOLERANCE,
                     (float) 50.0);

    setnurbsproperty(GL2.GL_MAP1_VERTEX_3, NurbsConsts.N_ERROR_TOLERANCE,
                     (float) 0.50);
    setnurbsproperty(GL2.GL_MAP1_VERTEX_4, NurbsConsts.N_ERROR_TOLERANCE,
                     (float) 0.50);
    setnurbsproperty(GL2.GL_MAP2_VERTEX_3, NurbsConsts.N_ERROR_TOLERANCE,
                     (float) 0.50);
    setnurbsproperty(GL2.GL_MAP2_VERTEX_4, NurbsConsts.N_ERROR_TOLERANCE,
                     (float) 0.50);

    setnurbsproperty(GL2.GL_MAP1_VERTEX_3, NurbsConsts.N_S_STEPS,
                     (float) 100.0);
    setnurbsproperty(GL2.GL_MAP1_VERTEX_4, NurbsConsts.N_S_STEPS,
                     (float) 100.0);
    setnurbsproperty(GL2.GL_MAP2_VERTEX_3, NurbsConsts.N_S_STEPS,
                     (float) 100.0);
    setnurbsproperty(GL2.GL_MAP2_VERTEX_4, NurbsConsts.N_S_STEPS,
                     (float) 100.0);

    setnurbsproperty(GL2.GL_MAP1_VERTEX_3, NurbsConsts.N_SAMPLINGMETHOD,
                     NurbsConsts.N_PATHLENGTH);

    set_domain_distance_u_rate(100.0);
    set_domain_distance_v_rate(100.0);
    set_is_domain_distance_sampling(0);

    this.autoloadmode = true;

    this.callBackFlag = 0;

    this.errorCallback = null;
  }

  /**
   * Sets domain distance for dom.dist. sampling in u direction
   * 
   * @param d
   *            distance
   */
  private void set_domain_distance_u_rate(double d) {
    // DONE
    subdivider.set_domain_distance_u_rate(d);
  }

  /**
   * Sets domain distance for dom.dist. sampling in v direction
   * 
   * @param d
   *            distance
   */
  private void set_domain_distance_v_rate(double d) {
    // DONE
    subdivider.set_domain_distance_v_rate(d);
  }

  /**
   * Begins new NURBS curve
   */
  public void bgncurve() {
    // DONE
    O_curve o_curve = new O_curve();
    thread("do_bgncurve", o_curve);
  }

  /**
   * Calls a method with given name and passes argumet
   * 
   * @param name
   *            name of a method to be called
   * @param arg
   *            parameter to be passed to called method
   */
  private void thread(String name, Object arg) {
    // DONE
    Class partype[] = new Class[1];
    partype[0] = arg.getClass();
    Method m;
    try {
      m = this.getClass().getMethod(name, partype);
      if (dl != null) {
        dl.append(this, m, arg);
      } else {
        m.invoke(this, new Object[] { arg });
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }

  }

  /**
   * Calls a method with given name
   * 
   * @param name
   *            name of a method to be called
   */
  private void thread2(String name) {
    // DONE
    try {
      Method m = this.getClass().getMethod(name, (Class[]) null);
      if (dl != null) {
        dl.append(this, m, null);
      } else {
        m.invoke(this, (Object[]) null);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * Begins a NURBS curve
   * 
   * @param o_curve
   *            curve object
   */
  public void do_bgncurve(O_curve o_curve) {
    if (inCurve > 0) {
      do_nurbserror(6);
      endcurve();
    }
    inCurve = 1;
    currentCurve = o_curve;

    currentCurve.curvetype = CT_NONE;

    if (inTrim) {
      if (!nextCurve.equals(o_curve)) {
        isCurveModified = 1;
        nextCurve = o_curve;
      }
    } else {
      if (!playBack)
        bgnrender();
      isDataValid = 1;
    }
    nextCurve = o_curve.next;
    // kind of solution of union
    nextPwlcurve = o_curve.o_pwlcurve;
    nextNurbscurve = o_curve.o_nurbscurve;
  }

  /**
   * Begins new surface
   * 
   * @param o_surface
   *            surface object
   */
  public void do_bgnsurface(O_surface o_surface) {
    // DONE
    if (inSurface > 0) {
      do_nurbserror(27);
      endsurface();
    }
    inSurface = 1;
    if (!playBack)
      bgnrender();

    isTrimModified = false;
    isDataSurfaceModified = false;
    isDataValid = 1;
    numTrims = 0;
    currentSurface = o_surface;
    nextTrim = o_surface.o_trim;
    nextNurbssurface = o_surface.o_nurbssurface;
  }

  /**
   * End a curve
   */
  public void endcurve() {
    // DONE
    thread2("do_endcurve");
  }

  /**
   * Ends surface
   */
  public void do_endsurface() {
    // DONE
    if (inTrim) {
      do_nurbserror(12);
      endtrim();
    }

    if (inSurface <= 0) {
      do_nurbserror(13);
      return;
    }

    inSurface = 0;

    nextNurbssurface = null;

    if (isDataValid <= 0) {
      return;
    }

    if (nextTrim != null) {
      isTrimModified = true;
      nextTrim = null;
    }

    // TODO errval ??
    if (numTrims > 0) {
      //                System.out.println("TODO glunurbs.do_endsurface - numtrims > 0");
    }

    subdivider.beginQuilts(new GL2Backend());
    for (O_nurbssurface n = currentSurface.o_nurbssurface; n != null; n = n.next) {
      subdivider.addQuilt(n.bezier_patches);
    }
    subdivider.endQuilts();
    subdivider.drawSurfaces();
    if (!playBack)
      endrender();

  }

  /**
   * Ends a curve
   */
  public void do_endcurve() {
    // DONE
    //            // System.out.println("do_endcurve");
    if (inCurve <= 0) {
      do_nurbserror(7);
      return;
    }
    inCurve = 0;

    nextCurve = null;

    if (currentCurve.curvetype == CT_NURBSCURVE) {
      // nextNurbscurve = null;
      // currentCurve.o_nurbscurve=null;
    } else {
      // nextPwlcurve = null;
      // currentCurve.o_pwlcurve=null;
    }
    if (!inTrim) {
      if (isDataValid <= 0) {
        return;
      }
      // TODO errval?
      if (currentCurve.curvetype == CT_NURBSCURVE) {
        subdivider.beginQuilts(new GL2Backend());

        for (O_nurbscurve n = currentCurve.o_nurbscurve; n != null; n = n.next)
          subdivider.addQuilt(n.bezier_curves);

        subdivider.endQuilts();
        subdivider.drawCurves();
        if (!playBack)
          endrender();
      } else {
        if (!playBack)
          endrender();
        do_nurbserror(9);
      }
    }

  }

  /**
   * Method for handling error codes
   * 
   * @param i
   *            error code
   */
  private void do_nurbserror(int i) {
    // TODO nurberror
    //            System.out.println("TODO nurbserror " + i);
  }

  /**
   * Begin rendering
   */
  private void bgnrender() {
    // DONE
    if (autoloadmode) {
      loadGLMatrices();
    }
  }

  /**
   * Load matrices from OpenGL state machine
   */
  private void loadGLMatrices() {
    // TODO Auto-generated method stub
    //            System.out.println("TODO glunurbs.loadGLMatrices");
  }

  /**
   * End rendering
   */
  private void endrender() {
    // DONE
  }

  /**
   * Make a NURBS curve
   * 
   * @param nknots
   *            number of knots in knot vector
   * @param knot
   *            knot vector
   * @param stride
   *            number of control points coordinates
   * @param ctlarray
   *            control points
   * @param order
   *            order of the curve
   * @param realType
   *            type of the curve
   */
  public void nurbscurve(int nknots, float[] knot, int stride,
                         float[] ctlarray, int order, int realType) {
    // DONE
    Mapdesc mapdesc = maplist.locate(realType);
    if (mapdesc == null) {
      do_nurbserror(35);
      isDataValid = 0;
      return;
    }
    if (ctlarray == null) {
      do_nurbserror(36);
      isDataValid = 0;
      return;
    }
    if (stride < 0) {
      do_nurbserror(34);
      isDataValid = 0;
      return;
    }
    Knotvector knots = new Knotvector(nknots, stride, order, knot);

    if (!do_check_knots(knots, "curve"))
      return;

    O_nurbscurve o_nurbscurve = new O_nurbscurve(realType);
    o_nurbscurve.bezier_curves = new Quilt(mapdesc);
    CArrayOfFloats ctrlcarr = new CArrayOfFloats(ctlarray);
    o_nurbscurve.bezier_curves.toBezier(knots, ctrlcarr, mapdesc
                                        .getNCoords());
    thread("do_nurbscurve", o_nurbscurve);
  }

  /**
   * Check knot vector specification
   * 
   * @param knots
   *            knot vector
   * @param msg
   *            error message
   * @return knot vector is / is not valid
   */
  public boolean do_check_knots(Knotvector knots, String msg) {
    // DONE
    int status = knots.validate();
    if (status > 0) {
      do_nurbserror(status);
      if (renderhints.errorchecking != NurbsConsts.N_NOMSG)
        knots.show(msg);
    }
    return (status > 0) ? false : true;
  }

  /**
   * Draw a curve
   * 
   * @param o_nurbscurve
   *            NURBS curve object
   */
  public void do_nurbscurve(O_nurbscurve o_nurbscurve) {
    // DONE

    if (inCurve <= 0) {
      bgncurve();
      inCurve = 2;
    }

    if (o_nurbscurve.used) {
      do_nurbserror(23);
      isDataValid = 0;
      return;
    } else
      o_nurbscurve.used = true;

    if (currentCurve.curvetype == CT_NONE) {
      currentCurve.curvetype = CT_NURBSCURVE;
    } else if (currentCurve.curvetype != CT_NURBSCURVE) {
      do_nurbserror(24);
      isDataValid = 0;
      return;
    }

    // it was necessary to overcome problem with pointer to pointer here

    // if(!o_nurbscurve.equals(nextNurbscurve)){
    if (!o_nurbscurve.equals(currentCurve.o_nurbscurve)) {
      isCurveModified = 1;
      currentCurve.o_nurbscurve = o_nurbscurve;
      // nextNurbscurve=o_nurbscurve;

    }

    nextNurbscurve = o_nurbscurve.next;

    if (!currentCurve.equals(o_nurbscurve.owner)) {
      isCurveModified = 1;
      o_nurbscurve.owner = currentCurve;
    }

    if (o_nurbscurve.owner == null)
      isCurveModified = 1;

    if (inCurve == 2)
      endcurve();
  }

  /**
   * Draw NURBS surface
   * 
   * @param o_nurbssurface
   *            NURBS surface object
   */
  public void do_nurbssurface(O_nurbssurface o_nurbssurface) {
    // DONE
    if (inSurface <= 0) {
      bgnsurface();
      inSurface = 2;
    }
    if (o_nurbssurface.used) {
      do_nurbserror(25);
      isDataValid = 0;
      return;
    } else
      o_nurbssurface.used = true;

    if (!o_nurbssurface.equals(nextNurbscurve)) {
      isSurfaceModified = true;
      // nextNurbssurface=o_nurbssurface;
      currentSurface.o_nurbssurface = o_nurbssurface;
    }

    if (!currentSurface.equals(o_nurbssurface.owner)) {
      isSurfaceModified = true;
      o_nurbssurface.owner = currentSurface;
    }

    nextNurbssurface = o_nurbssurface.next;

    if (inSurface == 2)
      endsurface();
  }

  /**
   * (Re)Inicialize maps
   */
  public void redefineMaps() {
    // DONE
    maplist.initialize();
  }

  /**
   * Define a map of given properties
   * 
   * @param type
   *            map type
   * @param rational
   *            is rational
   * @param ncoords
   *            number of control point coordinates
   */
  public void defineMap(int type, int rational, int ncoords) {
    // DONE
    maplist.define(type, rational, ncoords);
  }

  /**
   * Set NURBS property
   * 
   * @param type
   *            property type
   * @param tag
   *            property tag
   * @param value
   *            property value
   */
  public void setnurbsproperty(int type, int tag, float value) {
    // DONE
    Mapdesc mapdesc = maplist.locate(type);
    if (mapdesc == null) {
      do_nurbserror(35);
      return;
    }
    if (!mapdesc.isProperty(tag)) {
      do_nurbserror(26);
      return;
    }
    Property prop = new Property(type, tag, value);
    thread("do_setnurbsproperty2", prop);
  }

  /**
   * Set parameters of existing property
   * 
   * @param prop
   *            property
   */
  public void do_setnurbsproperty2(Property prop) {
    Mapdesc mapdesc = maplist.find(prop.type);
    mapdesc.setProperty(prop.tag, prop.value);
  }

  /**
   * Set given property to rendering hints
   * 
   * @param prop
   *            property to be set
   */
  public void do_setnurbsproperty(Property prop) {
    // DONE
    renderhints.setProperty(prop);
    // TODO freeproperty?
  }

  /**
   * Sets wheteher we use domain distance sampling
   * 
   * @param i
   *            domain distance sampling flag
   */
  public void set_is_domain_distance_sampling(int i) {
    // DONE
    subdivider.set_is_domain_distance_sampling(i);
  }

  /**
   * Begin new surface
   */
  public void bgnsurface() {
    // DONE
    O_surface o_surface = new O_surface();
    // TODO nuid
    //            System.out.println("TODO glunurbs.bgnsurface nuid");
    thread("do_bgnsurface", o_surface);
  }

  /**
   * End current surface
   */
  public void endsurface() {
    // DONE
    thread2("do_endsurface");
  }

  /**
   * End surface trimming
   */
  private void endtrim() {
    // TODO Auto-generated method stub
    //            System.out.println("TODO glunurbs.endtrim");
  }

  /**
   * Make NURBS surface
   * 
   * @param sknot_count
   *            number of knots in s direction
   * @param sknot
   *            knot vector in s direction
   * @param tknot_count
   *            number of knots in t direction
   * @param tknot
   *            knot vector in t direction
   * @param s_stride
   *            number of coords of control points in s direction
   * @param t_stride
   *            number of coords of control points in t direction
   * @param ctlarray
   *            control points
   * @param sorder
   *            order of curve in s direction
   * @param torder
   *            order of curve in t direction
   * @param type
   *            NURBS surface type (rational,...)
   */
  public void nurbssurface(int sknot_count, float[] sknot, int tknot_count,
                           float[] tknot, int s_stride, int t_stride, float[] ctlarray,
                           int sorder, int torder, int type) {
    // DONE
    Mapdesc mapdesc = maplist.locate(type);
    if (mapdesc == null) {
      do_nurbserror(35);
      isDataValid = 0;
      return;
    }
    if (s_stride < 0 || t_stride < 0) {
      do_nurbserror(34);
      isDataValid = 0;
      return;
    }
    Knotvector sknotvector = new Knotvector(sknot_count, s_stride, sorder,
                                            sknot);
    if (!do_check_knots(sknotvector, "surface"))
      return;
    Knotvector tknotvector = new Knotvector(tknot_count, t_stride, torder,
                                            tknot);
    if (!do_check_knots(tknotvector, "surface"))
      return;

    O_nurbssurface o_nurbssurface = new O_nurbssurface(type);
    o_nurbssurface.bezier_patches = new Quilt(mapdesc);

    CArrayOfFloats ctrlarr = new CArrayOfFloats(ctlarray);
    o_nurbssurface.bezier_patches.toBezier(sknotvector, tknotvector,
                                           ctrlarr, mapdesc.getNCoords());
    thread("do_nurbssurface", o_nurbssurface);
  }
}
